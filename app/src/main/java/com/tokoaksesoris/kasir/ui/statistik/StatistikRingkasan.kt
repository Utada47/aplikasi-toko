package com.tokoaksesoris.kasir.ui.statistik

/**
 * Model generik satu baris ringkasan pada layar Statistik.
 * Dipakai untuk ketiga mode tampilan: Harian, Bulanan, dan Tahunan --
 * bentuknya sama, cuma cara pengelompokan & label yang beda.
 */
data class StatistikRingkasan(
    val key: String,                    // kunci unik baris ini (untuk DiffUtil), misal "2026-09-04" / "2026-09" / "2026"
    val label: String,                  // teks tanggal/bulan/tahun yang sudah diformat, misal "Kamis, 4 September 2026"
    val subtitle: String,                // teks kecil di bawah label, misal "12 item · Selesai" atau "18 hari transaksi"
    val sessionIds: List<Long>,
    val representativeTimestamp: Long,  // dipakai saat buka DetailHarianActivity
    val total: Double,
    val totalBarang: Double,
    val jumlahBarang: Int,
    val totalPulsa: Double,
    val jumlahPulsa: Int
) {
    val jumlahItem: Int get() = jumlahBarang + jumlahPulsa
}
