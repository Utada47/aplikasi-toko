package com.tokoaksesoris.kasir.ui.statistik

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tokoaksesoris.kasir.data.Repository
import com.tokoaksesoris.kasir.data.Session
import com.tokoaksesoris.kasir.data.TipeTransaksi
import com.tokoaksesoris.kasir.data.TransaksiItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TreeMap

class StatistikViewModel(private val repo: Repository) : ViewModel() {

    private val _data = MutableLiveData<List<StatistikHarian>>(emptyList())
    val data: LiveData<List<StatistikHarian>> = _data

    private val _insight = MutableLiveData<RingkasanInsight>()
    val insight: LiveData<RingkasanInsight> = _insight

    private val _barangTerlaris = MutableLiveData<List<BarangTerlaris>>(emptyList())
    val barangTerlaris: LiveData<List<BarangTerlaris>> = _barangTerlaris

    private val _chartData = MutableLiveData<List<Pair<String, Double>>>(emptyList())
    val chartData: LiveData<List<Pair<String, Double>>> = _chartData

    private val _rentang = MutableLiveData(RentangWaktu.TUJUH_HARI)
    val rentang: LiveData<RentangWaktu> = _rentang

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale("in", "ID"))
    private val chartLabelFormat = SimpleDateFormat("d/M", Locale("in", "ID"))

    fun setRentang(r: RentangWaktu) {
        _rentang.value = r
        muatUlang()
    }

    fun muatUlang() {
        viewModelScope.launch {
            val sessions = repo.getAllSessions()
            val allItems = repo.getAllTransaksi()
            val itemsBySession = allItems.groupBy { it.sessionId }

            // ---- Kelompokkan per tanggal mulai sesi (hari A tetap hari A meski ditutup hari C) ----
            val perTanggalSemua = TreeMap<String, MutableList<Session>>(compareByDescending { it })
            for (s in sessions) {
                val key = dateKeyFormat.format(Date(s.tanggalMulai))
                perTanggalSemua.getOrPut(key) { mutableListOf() }.add(s)
            }

            fun hitungHarian(sesiList: List<Session>): StatistikHarian {
                var totalBarang = 0.0
                var jumlahBarang = 0
                var totalPulsa = 0.0
                var jumlahPulsa = 0
                var adaYangTerbuka = false

                for (s in sesiList) {
                    val items = itemsBySession[s.id].orEmpty()
                    for (item in items) {
                        if (item.tipe == TipeTransaksi.BARANG) {
                            totalBarang += item.harga; jumlahBarang++
                        } else {
                            totalPulsa += item.harga; jumlahPulsa++
                        }
                    }
                    if (s.isOpen) adaYangTerbuka = true
                }

                return StatistikHarian(
                    sessionId = sesiList.first().id,
                    sessionIds = sesiList.map { it.id },
                    tanggalMulai = sesiList.first().tanggalMulai,
                    total = totalBarang + totalPulsa,
                    jumlahItem = jumlahBarang + jumlahPulsa,
                    totalBarang = totalBarang,
                    jumlahBarang = jumlahBarang,
                    totalPulsa = totalPulsa,
                    jumlahPulsa = jumlahPulsa,
                    statusText = if (adaYangTerbuka) "Sedang berjalan" else "Selesai"
                )
            }

            // ---- Insight: Hari ini vs Kemarin vs rata-rata 7 hari (SELALU berbasis semua data, tidak ikut filter) ----
            val kalender = Calendar.getInstance()
            val keyHariIni = dateKeyFormat.format(kalender.time)
            kalender.add(Calendar.DAY_OF_YEAR, -1)
            val keyKemarin = dateKeyFormat.format(kalender.time)

            val totalHariIni = perTanggalSemua[keyHariIni]?.let { hitungHarian(it).total } ?: 0.0
            val totalKemarin = perTanggalSemua[keyKemarin]?.let { hitungHarian(it).total } ?: 0.0

            val tujuhHariTerakhir = (0..6).map { i ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                dateKeyFormat.format(cal.time)
            }
            val totalTujuhHari = tujuhHariTerakhir.sumOf { key -> perTanggalSemua[key]?.let { hitungHarian(it).total } ?: 0.0 }
            val rataRata7 = totalTujuhHari / 7.0

            _insight.postValue(RingkasanInsight(totalHariIni, totalKemarin, rataRata7))

            // ---- Terapkan filter rentang waktu untuk daftar, grafik & barang terlaris ----
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
                    perTanggalSemua.forEach { (key, list) ->
                        if (list.first().tanggalMulai >= cutoff) put(key, list)
                    }
                }
            }

            val hasilList = perTanggalTerfilter.map { (_, sesiList) -> hitungHarian(sesiList) }
            _data.postValue(hasilList)

            // ---- Grafik: urut kronologis (lama -> baru), maksimal 30 titik supaya tetap enak dilihat ----
            val chartEntries = hasilList
                .sortedBy { it.tanggalMulai }
                .takeLast(30)
                .map { chartLabelFormat.format(Date(it.tanggalMulai)) to it.total }
            _chartData.postValue(chartEntries)

            // ---- Barang terlaris (dari rentang waktu yang sama) ----
            val sessionIdsTerfilter = perTanggalTerfilter.values.flatten().map { it.id }.toSet()
            val itemsTerfilter = allItems.filter { it.sessionId in sessionIdsTerfilter }
            val terlaris = itemsTerfilter
                .groupBy { it.nama to it.tipe }
                .map { (key, items) ->
                    BarangTerlaris(
                        nama = key.first,
                        tipe = key.second,
                        jumlahTerjual = items.size,
                        totalPendapatan = items.sumOf { it.harga }
                    )
                }
                .sortedByDescending { it.totalPendapatan }
                .take(10)
            _barangTerlaris.postValue(terlaris)
        }
    }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StatistikViewModel(repo) as T
    }
}
