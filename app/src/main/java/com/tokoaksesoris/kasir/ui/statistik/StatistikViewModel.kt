package com.tokoaksesoris.kasir.ui.statistik

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tokoaksesoris.kasir.data.Repository
import com.tokoaksesoris.kasir.data.Session
import com.tokoaksesoris.kasir.data.TipeTransaksi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TreeMap

class StatistikViewModel(private val repo: Repository) : ViewModel() {

    private val _data = MutableLiveData<List<StatistikRingkasan>>(emptyList())
    val data: LiveData<List<StatistikRingkasan>> = _data

    private val _insight = MutableLiveData<RingkasanInsight>()
    val insight: LiveData<RingkasanInsight> = _insight

    private val _chartData = MutableLiveData<List<Pair<String, Double>>>(emptyList())
    val chartData: LiveData<List<Pair<String, Double>>> = _chartData

    private val _mode = MutableLiveData(ModeTampilan.HARIAN)
    val mode: LiveData<ModeTampilan> = _mode

    /**
     * Index pilihan filter rentang: 0 = opsi kecil, 1 = opsi besar, 2 = Semua.
     * Artinya beda tergantung mode aktif -- lihat [labelRentangUntukMode] dan [batasEntriUntukMode].
     * Untuk mode Tahunan filter ini tidak dipakai (selalu tampil semua tahun).
     */
    private val _rentangIndex = MutableLiveData(0)
    val rentangIndex: LiveData<Int> = _rentangIndex

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale("in", "ID"))
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale("in", "ID"))
    private val yearKeyFormat = SimpleDateFormat("yyyy", Locale("in", "ID"))

    private val labelHarianFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("in", "ID"))
    private val labelBulananFormat = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
    private val chartLabelHarianFormat = SimpleDateFormat("d/M", Locale("in", "ID"))
    private val chartLabelBulananFormat = SimpleDateFormat("MMM yy", Locale("in", "ID"))

    fun setRentangIndex(index: Int) {
        _rentangIndex.value = index
        muatUlang()
    }

    fun setMode(m: ModeTampilan) {
        _mode.value = m
        _rentangIndex.value = 0 // reset ke opsi pertama tiap ganti mode, supaya tidak membingungkan
        muatUlang()
    }

    /** Label 2 tombol filter rentang, menyesuaikan mode aktif. */
    fun labelRentangUntukMode(m: ModeTampilan): Pair<String, String> = when (m) {
        ModeTampilan.HARIAN -> "7 Hari" to "30 Hari"
        ModeTampilan.BULANAN -> "6 Bulan" to "12 Bulan"
        ModeTampilan.TAHUNAN -> "5 Tahun" to "10 Tahun"
    }

    private fun batasEntriUntukMode(m: ModeTampilan, index: Int): Int = when (m) {
        ModeTampilan.HARIAN -> if (index == 1) 30 else 7
        ModeTampilan.BULANAN -> if (index == 1) 12 else 6
        ModeTampilan.TAHUNAN -> if (index == 1) 10 else 5
    }

    fun muatUlang() {
        viewModelScope.launch {
            val sessions = repo.getAllSessions()
            val allItems = repo.getAllTransaksi()
            val itemsBySession = allItems.groupBy { it.sessionId }

            data class Agregat(
                val totalBarang: Double, val jumlahBarang: Int,
                val totalPulsa: Double, val jumlahPulsa: Int,
                val adaTerbuka: Boolean
            )

            fun hitungAgregat(sesiList: List<Session>): Agregat {
                var totalBarang = 0.0; var jumlahBarang = 0
                var totalPulsa = 0.0; var jumlahPulsa = 0
                var adaTerbuka = false
                for (s in sesiList) {
                    for (item in itemsBySession[s.id].orEmpty()) {
                        if (item.tipe == TipeTransaksi.BARANG) {
                            totalBarang += item.harga; jumlahBarang++
                        } else {
                            totalPulsa += item.harga; jumlahPulsa++
                        }
                    }
                    if (s.isOpen) adaTerbuka = true
                }
                return Agregat(totalBarang, jumlahBarang, totalPulsa, jumlahPulsa, adaTerbuka)
            }

            // ---- Kelompokkan per HARI (tanggal mulai sesi -- hari A tetap hari A meski ditutup hari C) ----
            val perTanggalSemua = TreeMap<String, MutableList<Session>>(compareByDescending { it })
            for (s in sessions) {
                val key = dateKeyFormat.format(Date(s.tanggalMulai))
                perTanggalSemua.getOrPut(key) { mutableListOf() }.add(s)
            }

            // ---- Insight: Hari ini vs Kemarin vs rata-rata 7 hari (selalu berbasis SEMUA data, tak terpengaruh filter) ----
            val kalender = Calendar.getInstance()
            val keyHariIni = dateKeyFormat.format(kalender.time)
            kalender.add(Calendar.DAY_OF_YEAR, -1)
            val keyKemarin = dateKeyFormat.format(kalender.time)

            val totalHariIni = perTanggalSemua[keyHariIni]?.let { hitungAgregat(it).let { a -> a.totalBarang + a.totalPulsa } } ?: 0.0
            val totalKemarin = perTanggalSemua[keyKemarin]?.let { hitungAgregat(it).let { a -> a.totalBarang + a.totalPulsa } } ?: 0.0

            val tujuhHariTerakhir = (0..6).map { i ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                dateKeyFormat.format(cal.time)
            }
            val totalTujuhHari = tujuhHariTerakhir.sumOf { key ->
                perTanggalSemua[key]?.let { hitungAgregat(it).let { a -> a.totalBarang + a.totalPulsa } } ?: 0.0
            }
            _insight.postValue(RingkasanInsight(totalHariIni, totalKemarin, totalTujuhHari / 7.0))

            // ---- Bangun daftar ringkasan sesuai MODE tampilan, lalu batasi sesuai filter rentang ----
            val modeSekarang = _mode.value ?: ModeTampilan.HARIAN
            val indexSekarang = _rentangIndex.value ?: 0
            val batasEntri = batasEntriUntukMode(modeSekarang, indexSekarang)

            val hasilLengkap: List<StatistikRingkasan> = when (modeSekarang) {
                ModeTampilan.HARIAN -> {
                    perTanggalSemua.map { (key, sesiList) ->
                        val a = hitungAgregat(sesiList)
                        val tgl = sesiList.first().tanggalMulai
                        StatistikRingkasan(
                            key = key,
                            label = labelHarianFormat.format(Date(tgl)),
                            subtitle = "${a.jumlahBarang + a.jumlahPulsa} item total · ${if (a.adaTerbuka) "Sedang berjalan" else "Selesai"}",
                            sessionIds = sesiList.map { it.id },
                            representativeTimestamp = tgl,
                            total = a.totalBarang + a.totalPulsa,
                            totalBarang = a.totalBarang, jumlahBarang = a.jumlahBarang,
                            totalPulsa = a.totalPulsa, jumlahPulsa = a.jumlahPulsa
                        )
                    }
                }
                ModeTampilan.BULANAN -> {
                    val perBulan = TreeMap<String, MutableList<Session>>(compareByDescending { it })
                    for (s in sessions) {
                        val key = monthKeyFormat.format(Date(s.tanggalMulai))
                        perBulan.getOrPut(key) { mutableListOf() }.add(s)
                    }
                    perBulan.map { (key, sesiList) ->
                        val a = hitungAgregat(sesiList)
                        val tgl = sesiList.first().tanggalMulai
                        val jumlahHariUnik = sesiList.map { dateKeyFormat.format(Date(it.tanggalMulai)) }.distinct().size
                        StatistikRingkasan(
                            key = key,
                            label = labelBulananFormat.format(Date(tgl)),
                            subtitle = "$jumlahHariUnik hari transaksi · ${a.jumlahBarang + a.jumlahPulsa} item",
                            sessionIds = sesiList.map { it.id },
                            representativeTimestamp = tgl,
                            total = a.totalBarang + a.totalPulsa,
                            totalBarang = a.totalBarang, jumlahBarang = a.jumlahBarang,
                            totalPulsa = a.totalPulsa, jumlahPulsa = a.jumlahPulsa
                        )
                    }
                }
                ModeTampilan.TAHUNAN -> {
                    val perTahun = TreeMap<String, MutableList<Session>>(compareByDescending { it })
                    for (s in sessions) {
                        val key = yearKeyFormat.format(Date(s.tanggalMulai))
                        perTahun.getOrPut(key) { mutableListOf() }.add(s)
                    }
                    perTahun.map { (key, sesiList) ->
                        val a = hitungAgregat(sesiList)
                        val tgl = sesiList.first().tanggalMulai
                        val jumlahBulanUnik = sesiList.map { monthKeyFormat.format(Date(it.tanggalMulai)) }.distinct().size
                        StatistikRingkasan(
                            key = key,
                            label = key, // "2026"
                            subtitle = "$jumlahBulanUnik bulan transaksi · ${a.jumlahBarang + a.jumlahPulsa} item",
                            sessionIds = sesiList.map { it.id },
                            representativeTimestamp = tgl,
                            total = a.totalBarang + a.totalPulsa,
                            totalBarang = a.totalBarang, jumlahBarang = a.jumlahBarang,
                            totalPulsa = a.totalPulsa, jumlahPulsa = a.jumlahPulsa
                        )
                    }
                }
            }

            // hasilLengkap sudah terurut menurun (terbaru dulu) karena TreeMap compareByDescending;
            // "N entri terakhir" = ambil N item PERTAMA dari daftar yang menurun ini.
            val hasilList = hasilLengkap.take(batasEntri)
            _data.postValue(hasilList)

            // ---- Grafik: kronologis lama->baru, dari daftar yang sudah dibatasi filter di atas ----
            // Pengaman tambahan khusus grafik: kalau "Semua" menghasilkan entri sangat banyak,
            // grafik tetap dibatasi biar tidak penuh sesak (daftar/list di bawahnya tetap lengkap).
            val batasAmanGrafik = when (modeSekarang) {
                ModeTampilan.HARIAN -> 30
                ModeTampilan.BULANAN -> 24
                ModeTampilan.TAHUNAN -> 10
            }
            val chartFormat = if (modeSekarang == ModeTampilan.HARIAN) chartLabelHarianFormat else chartLabelBulananFormat
            val chartEntries = hasilList
                .sortedBy { it.representativeTimestamp }
                .takeLast(batasAmanGrafik)
                .map { ringkasan ->
                    val labelChart = if (modeSekarang == ModeTampilan.TAHUNAN) ringkasan.label
                    else chartFormat.format(Date(ringkasan.representativeTimestamp))
                    labelChart to ringkasan.total
                }
            _chartData.postValue(chartEntries)
        }
    }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StatistikViewModel(repo) as T
    }
}
