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

    private val _barangTerlaris = MutableLiveData<List<BarangTerlaris>>(emptyList())
    val barangTerlaris: LiveData<List<BarangTerlaris>> = _barangTerlaris

    private val _chartData = MutableLiveData<List<Pair<String, Double>>>(emptyList())
    val chartData: LiveData<List<Pair<String, Double>>> = _chartData

    private val _rentang = MutableLiveData(RentangWaktu.TUJUH_HARI)
    val rentang: LiveData<RentangWaktu> = _rentang

    private val _mode = MutableLiveData(ModeTampilan.HARIAN)
    val mode: LiveData<ModeTampilan> = _mode

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale("in", "ID"))
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale("in", "ID"))
    private val yearKeyFormat = SimpleDateFormat("yyyy", Locale("in", "ID"))

    private val labelHarianFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("in", "ID"))
    private val labelBulananFormat = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
    private val chartLabelHarianFormat = SimpleDateFormat("d/M", Locale("in", "ID"))
    private val chartLabelBulananFormat = SimpleDateFormat("MMM yy", Locale("in", "ID"))

    fun setRentang(r: RentangWaktu) {
        _rentang.value = r
        muatUlang()
    }

    fun setMode(m: ModeTampilan) {
        _mode.value = m
        muatUlang()
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

            // ---- Insight: Hari ini vs Kemarin vs rata-rata 7 hari (selalu berbasis semua data) ----
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

            // ---- Barang terlaris: berdasarkan filter rentang waktu (7/30/Semua), independen dari mode tampilan ----
            val rentangSekarang = _rentang.value ?: RentangWaktu.TUJUH_HARI
            val batasHari = rentangSekarang.jumlahHari
            val perTanggalTerfilter = if (batasHari == null) {
                perTanggalSemua
            } else {
                val cutoffCal = Calendar.getInstance()
                cutoffCal.add(Calendar.DAY_OF_YEAR, -(batasHari - 1))
                cutoffCal.set(Calendar.HOUR_OF_DAY, 0); cutoffCal.set(Calendar.MINUTE, 0)
                cutoffCal.set(Calendar.SECOND, 0); cutoffCal.set(Calendar.MILLISECOND, 0)
                val cutoff = cutoffCal.timeInMillis
                TreeMap<String, MutableList<Session>>(compareByDescending { it }).apply {
                    perTanggalSemua.forEach { (key, list) -> if (list.first().tanggalMulai >= cutoff) put(key, list) }
                }
            }
            val sessionIdsTerfilter = perTanggalTerfilter.values.flatten().map { it.id }.toSet()
            val itemsTerfilter = allItems.filter { it.sessionId in sessionIdsTerfilter }
            val terlaris = itemsTerfilter
                .groupBy { it.nama to it.tipe }
                .map { (key, items) ->
                    BarangTerlaris(nama = key.first, tipe = key.second, jumlahTerjual = items.size, totalPendapatan = items.sumOf { it.harga })
                }
                .sortedByDescending { it.totalPendapatan }
                .take(10)
            _barangTerlaris.postValue(terlaris)

            // ---- Bangun daftar ringkasan sesuai MODE tampilan (Harian / Bulanan / Tahunan) ----
            val modeSekarang = _mode.value ?: ModeTampilan.HARIAN

            val hasilList: List<StatistikRingkasan> = when (modeSekarang) {
                ModeTampilan.HARIAN -> {
                    perTanggalTerfilter.map { (key, sesiList) ->
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
            _data.postValue(hasilList)

            // ---- Grafik mengikuti mode yang sedang aktif (kronologis lama->baru, dibatasi biar tetap enak dilihat) ----
            val batasTitikChart = when (modeSekarang) {
                ModeTampilan.HARIAN -> 30
                ModeTampilan.BULANAN -> 24
                ModeTampilan.TAHUNAN -> 10
            }
            val chartFormat = if (modeSekarang == ModeTampilan.HARIAN) chartLabelHarianFormat else chartLabelBulananFormat
            val chartEntries = hasilList
                .sortedBy { it.representativeTimestamp }
                .takeLast(batasTitikChart)
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
