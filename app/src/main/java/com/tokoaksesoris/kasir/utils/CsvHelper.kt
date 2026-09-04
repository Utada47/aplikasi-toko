package com.tokoaksesoris.kasir.utils

import android.content.Context
import android.net.Uri
import com.tokoaksesoris.kasir.data.Repository
import com.tokoaksesoris.kasir.data.Session
import com.tokoaksesoris.kasir.data.TipeTransaksi
import com.tokoaksesoris.kasir.data.TransaksiItem
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Format CSV (ringan, satu file, gabungan sesi + item):
 * row_type,session_id,tanggal_mulai,waktu_mulai,waktu_tutup,is_open,tipe,nama,harga,waktu_item
 *
 * row_type = SESSION -> kolom sesi terisi, kolom item kosong
 * row_type = ITEM    -> session_id merujuk ke sesi di atas, kolom item terisi
 *
 * Ini dipakai untuk:
 * 1) Export data untuk keperluan penelitian / backup.
 * 2) Import data dari versi aplikasi lama (asal formatnya mengikuti header yang sama,
 *    kolom yang tidak dikenal akan diabaikan, kolom yang hilang dianggap kosong).
 */
object CsvHelper {

    private const val HEADER = "row_type,session_id,tanggal_mulai,waktu_mulai,waktu_tutup,is_open,tipe,nama,harga,waktu_item"

    suspend fun exportToUri(context: Context, uri: Uri, repo: Repository) {
        val sessions = repo.getAllSessions()
        val items = repo.getAllTransaksi()

        context.contentResolver.openOutputStream(uri)?.use { out ->
            OutputStreamWriter(out).use { writer ->
                writer.appendLine(HEADER)
                for (s in sessions) {
                    writer.appendLine(
                        listOf(
                            "SESSION",
                            s.id.toString(),
                            s.tanggalMulai.toString(),
                            s.waktuMulai.toString(),
                            s.waktuTutup?.toString() ?: "",
                            if (s.isOpen) "1" else "0",
                            "", "", "", ""
                        ).joinToString(",") { csvEscape(it) }
                    )
                }
                for (i in items) {
                    writer.appendLine(
                        listOf(
                            "ITEM",
                            i.sessionId.toString(),
                            "", "", "", "",
                            i.tipe.name,
                            i.nama,
                            i.harga.toString(),
                            i.waktu.toString()
                        ).joinToString(",") { csvEscape(it) }
                    )
                }
            }
        }
    }

    /**
     * Import: seluruh isi CSV akan ditambahkan (bukan menimpa) ke database saat ini.
     * ID sesi lama dipetakan ulang ke ID baru supaya tidak bentrok dengan data yang sudah ada.
     * Semua sesi hasil import otomatis dianggap CLOSED (data historis),
     * kecuali kolom is_open pada file secara eksplisit menyatakan masih terbuka.
     */
    suspend fun importFromUri(context: Context, uri: Uri, repo: Repository): ImportResult {
        var jumlahSesi = 0
        var jumlahItem = 0
        val oldToNewSessionId = HashMap<Long, Long>()

        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                val headerLine = reader.readLine() ?: return ImportResult(0, 0, "File CSV kosong")
                val columns = headerLine.split(",").map { it.trim() }
                val idx = columns.withIndex().associate { (i, name) -> name to i }

                fun col(fields: List<String>, name: String): String {
                    val i = idx[name] ?: return ""
                    return if (i < fields.size) fields[i] else ""
                }

                var line = reader.readLine()
                while (line != null) {
                    if (line.isNotBlank()) {
                        val fields = parseCsvLine(line)
                        when (col(fields, "row_type").uppercase()) {
                            "SESSION" -> {
                                val oldId = col(fields, "session_id").toLongOrNull()
                                val tanggalMulai = col(fields, "tanggal_mulai").toLongOrNull() ?: System.currentTimeMillis()
                                val waktuMulai = col(fields, "waktu_mulai").toLongOrNull() ?: tanggalMulai
                                val waktuTutup = col(fields, "waktu_tutup").toLongOrNull()
                                val isOpen = col(fields, "is_open") == "1"
                                val newId = repo.insertSessionRaw(
                                    Session(
                                        tanggalMulai = tanggalMulai,
                                        waktuMulai = waktuMulai,
                                        waktuTutup = waktuTutup,
                                        isOpen = isOpen
                                    )
                                )
                                if (oldId != null) oldToNewSessionId[oldId] = newId
                                jumlahSesi++
                            }
                            "ITEM" -> {
                                val oldSessionId = col(fields, "session_id").toLongOrNull()
                                val newSessionId = oldSessionId?.let { oldToNewSessionId[it] }
                                if (newSessionId != null) {
                                    val tipe = try {
                                        TipeTransaksi.valueOf(col(fields, "tipe").uppercase())
                                    } catch (e: Exception) {
                                        TipeTransaksi.BARANG
                                    }
                                    val nama = col(fields, "nama")
                                    val harga = col(fields, "harga").toDoubleOrNull() ?: 0.0
                                    val waktu = col(fields, "waktu_item").toLongOrNull() ?: System.currentTimeMillis()
                                    repo.insertTransaksiRaw(
                                        TransaksiItem(
                                            sessionId = newSessionId,
                                            tipe = tipe,
                                            nama = nama,
                                            harga = harga,
                                            waktu = waktu
                                        )
                                    )
                                    jumlahItem++
                                }
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
        } ?: return ImportResult(0, 0, "Tidak bisa membuka file")

        return ImportResult(jumlahSesi, jumlahItem, null)
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    sb.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    data class ImportResult(val jumlahSesi: Int, val jumlahItem: Int, val error: String?)
}
