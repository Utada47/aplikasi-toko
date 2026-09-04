package com.tokoaksesoris.kasir.ui.statistik

data class StatistikHarian(
    val sessionId: Long,
    val tanggalMulai: Long,
    val total: Double,
    val jumlahItem: Int,
    val statusText: String // "Sedang berjalan" atau "Selesai"
)
