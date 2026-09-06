package com.tokoaksesoris.kasir.ui.statistik

import com.tokoaksesoris.kasir.data.TipeTransaksi

data class BarangTerlaris(
    val nama: String,
    val tipe: TipeTransaksi,
    val jumlahTerjual: Int,
    val totalPendapatan: Double
)
