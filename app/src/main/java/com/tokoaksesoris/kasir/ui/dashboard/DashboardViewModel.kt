package com.tokoaksesoris.kasir.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.tokoaksesoris.kasir.data.Repository
import com.tokoaksesoris.kasir.data.Session
import com.tokoaksesoris.kasir.data.TipeTransaksi
import com.tokoaksesoris.kasir.data.TransaksiItem
import kotlinx.coroutines.launch

class DashboardViewModel(private val repo: Repository) : ViewModel() {

    val openSession: LiveData<Session?> = repo.observeOpenSession()

    private val sessionIdLiveData = MediatorLiveData<Long?>().apply {
        addSource(openSession) { value = it?.id }
    }

    val barangItems: LiveData<List<TransaksiItem>> = sessionIdLiveData.switchMap { id ->
        if (id == null) MutableLiveData<List<TransaksiItem>>(emptyList())
        else repo.observeItems(id, TipeTransaksi.BARANG)
    }

    val pulsaItems: LiveData<List<TransaksiItem>> = sessionIdLiveData.switchMap { id ->
        if (id == null) MutableLiveData<List<TransaksiItem>>(emptyList())
        else repo.observeItems(id, TipeTransaksi.PULSA)
    }

    val total: LiveData<Double> = sessionIdLiveData.switchMap { id ->
        if (id == null) MutableLiveData<Double>(0.0)
        else repo.observeTotal(id)
    }

    // Subtotal per tabel (dihitung dari list yang sudah observed, tanpa query tambahan)
    val totalBarang: LiveData<Double> = barangItems.map { list -> list.sumOf { it.harga } }
    val totalPulsa: LiveData<Double> = pulsaItems.map { list -> list.sumOf { it.harga } }

    fun mulaiHariIni() {
        viewModelScope.launch { repo.mulaiHariIni() }
    }

    fun tambahItem(tipe: TipeTransaksi, nama: String, harga: Double) {
        viewModelScope.launch {
            val session = openSession.value ?: repo.mulaiHariIni()
            repo.tambahItem(session.id, tipe, nama, harga)
        }
    }

    fun updateItem(item: TransaksiItem, tipe: TipeTransaksi, nama: String, harga: Double) {
        viewModelScope.launch {
            repo.updateItem(item.copy(tipe = tipe, nama = nama, harga = harga))
        }
    }

    fun tutupHari() {
        viewModelScope.launch {
            openSession.value?.let { repo.tutupHari(it) }
        }
    }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DashboardViewModel(repo) as T
    }
}
