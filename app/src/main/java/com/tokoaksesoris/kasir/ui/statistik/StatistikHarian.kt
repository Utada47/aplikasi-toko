package com.tokoaksesoris.kasir.ui.statistik

data class StatistikHarian(
    val sessionId: Long,
    val sessionIds: List<Long>,
    val tanggalMulai: Long,
    val total: Double,
    val jumlahItem: Int,
    val totalBarang: Double,
    val jumlahBarang: Int,
    val totalPulsa: Double,
    val jumlahPulsa: Int,
    val statusText: String // "Sedang berjalan" atau "Selesai"
)
