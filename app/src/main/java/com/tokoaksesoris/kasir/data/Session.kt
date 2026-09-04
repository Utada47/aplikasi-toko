package com.tokoaksesoris.kasir.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Merepresentasikan satu "sesi hari buka toko".
 * tanggalMulai = tanggal saat tombol "Mulai Buka Hari Ini" ditekan (hari A).
 * Jika sesi baru ditutup beberapa hari kemudian (hari C), semua transaksi
 * di dalamnya TETAP tercatat sebagai pemasukan tanggalMulai (hari A).
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanggalMulai: Long,      // epoch millis - tanggal mulai (dipakai untuk grouping statistik)
    val waktuMulai: Long,        // epoch millis - jam pasti mulai
    val waktuTutup: Long? = null,
    val isOpen: Boolean = true
)
