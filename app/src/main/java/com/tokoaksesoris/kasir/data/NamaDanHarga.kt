package com.tokoaksesoris.kasir.data

/**
 * Hasil query autocomplete: satu entry per nama unik,
 * dengan harga terakhir (waktu paling besar) yang pernah diinput.
 *
 * toString() = nama saja, supaya ArrayAdapter
 * otomatis menampilkan hanya nama di dropdown.
 */
data class NamaDanHarga(
    val nama: String,
    val harga: Double
) {
    override fun toString(): String = nama
}
