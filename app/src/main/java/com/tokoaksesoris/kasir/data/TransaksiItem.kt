package com.tokoaksesoris.kasir.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TipeTransaksi { BARANG, PULSA }

@Entity(
    tableName = "transaksi_items",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class TransaksiItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val tipe: TipeTransaksi,
    val nama: String,
    val harga: Double,
    val waktu: Long   // epoch millis saat item ditambahkan (dipakai untuk kolom "Jam")
)
