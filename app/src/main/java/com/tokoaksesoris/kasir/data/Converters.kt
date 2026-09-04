package com.tokoaksesoris.kasir.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTipe(tipe: TipeTransaksi): String = tipe.name

    @TypeConverter
    fun toTipe(value: String): TipeTransaksi = TipeTransaksi.valueOf(value)
}
