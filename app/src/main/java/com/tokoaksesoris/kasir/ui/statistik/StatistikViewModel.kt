package com.tokoaksesoris.kasir.ui.statistik

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tokoaksesoris.kasir.data.Repository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TreeMap

class StatistikViewModel(private val repo: Repository) : ViewModel() {

    private val _data = MutableLiveData<List<StatistikHarian>>(emptyList())
    val data: LiveData<List<StatistikHarian>> = _data

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale("in", "ID"))

    fun muatUlang() {
        viewModelScope.launch {
            val sessions = repo.getAllSessions()
            val allItems = repo.getAllTransaksi()
            val itemsBySession = allItems.groupBy { it.sessionId }

            // Kelompokkan per tanggal mulai sesi (hari A), bukan tanggal item,
            // supaya sesi yang ditutup di hari lain tetap tercatat sebagai
            // pemasukan hari sesi itu dimulai.
            val perTanggal = TreeMap<String, MutableList<com.tokoaksesoris.kasir.data.Session>>(compareByDescending { it })
            for (s in sessions) {
                val key = dateKeyFormat.format(java.util.Date(s.tanggalMulai))
                perTanggal.getOrPut(key) { mutableListOf() }.add(s)
            }

            val hasil = perTanggal.map { (tanggalKey, sesiList) ->
                var total = 0.0
                var jumlahItem = 0
                var adaYangTerbuka = false
                var tanggalMulaiRepresentatif = sesiList.first().tanggalMulai

                for (s in sesiList) {
                    val items = itemsBySession[s.id].orEmpty()
                    total += items.sumOf { it.harga }
                    jumlahItem += items.size
                    if (s.isOpen) adaYangTerbuka = true
                }

                StatistikHarian(
                    sessionId = sesiList.first().id,
                    tanggalMulai = tanggalMulaiRepresentatif,
                    total = total,
                    jumlahItem = jumlahItem,
                    statusText = if (adaYangTerbuka) "Sedang berjalan" else "Selesai"
                )
            }

            _data.postValue(hasil)
        }
    }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StatistikViewModel(repo) as T
    }
}
