package com.tokoaksesoris.kasir.ui.statistik

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tokoaksesoris.kasir.data.Repository
import com.tokoaksesoris.kasir.data.Session
import com.tokoaksesoris.kasir.data.TipeTransaksi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TreeMap

class StatistikViewModel(private val repo: Repository) : ViewModel() {

    companion object {
        /** Jumlah baris yang ditambahkan setiap kali user scroll sampai bawah. */
        private const val UKURAN_HALAMAN = 20
    }

    private val _data = MutableLiveData<List<StatistikRingkasan>>(emptyList())
    val data: LiveData<List<StatistikRingkasan>> = _data

    private val _insight = MutableLiveData<RingkasanInsight>()
    val insight: LiveData<RingkasanInsight> = _insight

    private val _chartData = MutableLiveData<List<Pair<String, Double>>>(emptyList())
    val chartData: LiveData<List<Pair<String, Double>>> = _chartData

    private val _mode = MutableLiveData(ModeTampilan.HARIAN)
    val mode: LiveData<ModeTampilan> = _mode

    /** true selagi memuat data pertama kali / ganti mode (bukan "muat lebih banyak"). */
    private val _sedangMuat = MutableLiveData(false)
    val sedangMuat: LiveData<Boolean> = _sedangMuat

    /** true selagi menambah halaman berikutnya (dipicu scroll sampai bawah). */
    private val _sedangMuatLebih = MutableLiveData(false)
    val sedangMuatLebih: LiveData<Boolean> = _sedangMuatLebih

    /** false kalau seluruh data sudah tertampilkan semua -- scroll tidak akan memuat apa-apa lagi. */
    private val _adaLebihBanyak = MutableLiveData(true)
    val adaLebihBanyak: LiveData<Boolean> = _adaLebihBanyak

    // Seluruh baris hasil pengelompokan utk mode yang sedang aktif, tersimpan penuh
    // di memori (perhitungan hanya sekali per ganti mode). "Muat lebih banyak" cuma
    // menampilkan potongan berikutnya dari cache ini -- TIDAK query ulang ke DB,
    // jadi sangat ringan meski dipanggil berkali-kali saat scroll.
    private var cacheLengkap: List<StatistikRingkasan> = emptyList()
    private var halamanSaatIni = 1

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale("in", "ID"))
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale("in", "ID"))
    private val yearKeyFormat = SimpleDateFormat("yyyy", Locale("in", "ID"))

    private val labelHarianFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("in", "ID"))
    private val labelBulananFormat = SimpleDateFormat("MMMM yyyy", Locale("in", "ID"))
    private val chartLabelHarianFormat = SimpleDateFormat("d/M", Locale("in", "ID"))
    private val chartLabelBulananFormat = SimpleDateFormat("MMM yy", Locale("in", "ID"))

    fun setMode(m: ModeTampilan) {
        _mode.value = m
        muatUlang()
    }

    /**
     * Muat ulang dari awal (dipanggil saat ganti mode, pertama kali dibuka, atau
     * ada perubahan data seperti item baru/edit/hapus). Query DB + pengelompokan
     * berat dijalankan di Dispatchers.Default supaya TIDAK PERNAH memblokir UI
     * thread, berapa pun banyaknya data (mencegah ANR).
     */
    fun muatUlang() {
        viewModelScope.launch {
            _sedangMuat.postValue(true)
            halamanSaatIni = 1

            val modeSekarang = _mode.value ?: ModeTampilan.HARIAN

            // Seluruh kerja berat (query + grouping) di background thread.
            val hasil = withContext(Dispatchers.Default) {
                hitungSemuaData(modeSekarang)
            }

            cacheLengkap = hasil.daftarLengkap
            _insight.postValue(hasil.insight)
            _chartData.postValue(hasil.chartData)

            tampilkanHalamanSaatIni()
            _sedangMuat.postValue(false)
        }
    }

    /**
     * Dipanggil saat user scroll sampai (hampir) bawah daftar. Karena cacheLengkap
     * sudah dihitung penuh sejak muatUlang(), ini HANYA slicing list di memori --
     * tidak ada query DB tambahan, jadi aman dipanggil sesering apapun saat scroll.
     */
    fun muatLebihBanyak() {
        if (_sedangMuat.value == true || _sedangMuatLebih.value == true) return
        if (_adaLebihBanyak.value != true) return

        viewModelScope.launch {
            _sedangMuatLebih.postValue(true)
            halamanSaatIni++
            tampilkanHalamanSaatIni()
            _sedangMuatLebih.postValue(false)
        }
    }

    private fun tampilkanHalamanSaatIni() {
        val batas = halamanSaatIni * UKURAN_HALAMAN
        _data.postValue(cacheLengkap.take(batas))
        _adaLebihBanyak.postValue(cacheLengkap.size > batas)
    }

    private class Agregat(
        val totalBarang: Double, val jumlahBarang: Int,
        val totalPulsa: Double, val jumlahPulsa: Int,
        val adaTerbuka: Boolean
    )

    private class HasilHitung(
        val daftarLengkap: List<StatistikRingkasan>,
        val insight: RingkasanInsight,
        val chartData: List<Pair<String, Double>>
    )

    /** Semua perhitungan berat (dipanggil dari background thread). */
    private suspend fun hitungSemuaData(modeSekarang: ModeTampilan): HasilHitung {
        val sessions = repo.getAllSessions()
        val allItems = repo.getAllTransaksi()
        val itemsBySession = allItems.groupBy { it.sessionId }

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

        // ---- Insight: Hari ini vs Kemarin vs rata-rata 7 hari (selalu berbasis SEMUA data) ----
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
        val insight = RingkasanInsight(totalHariIni, totalKemarin, totalTujuhHari / 7.0)

        // ---- Bangun daftar ringkasan LENGKAP (semua data, tanpa batas) sesuai mode ----
        val daftarLengkap: List<StatistikRingkasan> = when (modeSekarang) {
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

        // ---- Grafik: SELALU dari jendela terbaru yang wajar (independen dari pagination
        //      daftar di bawahnya) -- daftar boleh dimuat sedikit demi sedikit, tapi grafik
        //      cukup menunjukkan tren terkini saja ----
        val batasAmanGrafik = when (modeSekarang) {
            ModeTampilan.HARIAN -> 30
            ModeTampilan.BULANAN -> 24
            ModeTampilan.TAHUNAN -> 10
        }
        val chartFormat = if (modeSekarang == ModeTampilan.HARIAN) chartLabelHarianFormat else chartLabelBulananFormat
        val chartEntries = daftarLengkap
            .sortedBy { it.representativeTimestamp }
            .takeLast(batasAmanGrafik)
            .map { ringkasan ->
                val labelChart = if (modeSekarang == ModeTampilan.TAHUNAN) ringkasan.label
                else chartFormat.format(Date(ringkasan.representativeTimestamp))
                labelChart to ringkasan.total
            }

        return HasilHitung(daftarLengkap, insight, chartEntries)
    }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StatistikViewModel(repo) as T
    }
}
