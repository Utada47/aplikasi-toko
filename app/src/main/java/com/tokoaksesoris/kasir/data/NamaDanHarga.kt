package com.tokoaksesoris.kasir.data

/**
 * Hasil query autocomplete: satu entry per nama unik,
 * dengan harga terakhir (waktu paling besar) yang pernah diinput,
 * beserta tipe (Barang/Pulsa) dari histori terakhir nama tersebut --
 * dipakai supaya radio button otomatis ikut pindah kategori saat
 * user memilih nama dari histori.
 *
 * toString() = nama saja, supaya ArrayAdapter
 * otomatis menampilkan hanya nama di dropdown.
 */
data class NamaDanHarga(
    val nama: String,
    val harga: Double,
    val tipe: TipeTransaksi
) {
    override fun toString(): String = nama
}
