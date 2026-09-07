package com.tokoaksesoris.kasir.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AppDao {

    // ---------- Sessions ----------
    @Insert
    suspend fun insertSession(session: Session): Long

    @Update
    suspend fun updateSession(session: Session)

    @Query("SELECT * FROM sessions WHERE isOpen = 1 LIMIT 1")
    suspend fun getOpenSession(): Session?

    @Query("SELECT * FROM sessions WHERE isOpen = 1 LIMIT 1")
    fun observeOpenSession(): LiveData<Session?>

    @Query("SELECT * FROM sessions ORDER BY tanggalMulai DESC")
    fun observeAllSessions(): LiveData<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY tanggalMulai ASC")
    suspend fun getAllSessionsOnce(): List<Session>

    // ---------- Transaksi ----------
    @Insert
    suspend fun insertTransaksi(item: TransaksiItem): Long

    @Update
    suspend fun updateTransaksi(item: TransaksiItem)

    @Delete
    suspend fun deleteTransaksi(item: TransaksiItem)

    @Query("""
        SELECT * FROM transaksi_items
        WHERE sessionId = :sessionId AND tipe = :tipe
        ORDER BY waktu ASC
    """)
    fun observeItemsBySessionAndTipe(sessionId: Long, tipe: TipeTransaksi): LiveData<List<TransaksiItem>>

    @Query("SELECT COALESCE(SUM(harga), 0.0) FROM transaksi_items WHERE sessionId = :sessionId")
    fun observeTotalBySession(sessionId: Long): LiveData<Double>

    @Query("SELECT COALESCE(SUM(harga), 0.0) FROM transaksi_items WHERE sessionId = :sessionId")
    suspend fun getTotalBySessionOnce(sessionId: Long): Double

    @Query("SELECT * FROM transaksi_items ORDER BY waktu ASC")
    suspend fun getAllTransaksiOnce(): List<TransaksiItem>

    /**
     * Autocomplete (per tipe): kembalikan satu baris per nama unik dalam tipe tersebut,
     * beserta HARGA TERAKHIR (waktu terbesar) untuk nama itu.
     */
    @Query("""
        SELECT t.nama, t.harga, t.tipe
        FROM transaksi_items t
        INNER JOIN (
            SELECT nama, MAX(waktu) AS maxWaktu
            FROM transaksi_items
            WHERE tipe = :tipe
            GROUP BY nama
        ) latest ON t.nama = latest.nama
                 AND t.waktu = latest.maxWaktu
                 AND t.tipe = :tipe
        GROUP BY t.nama
        ORDER BY t.nama ASC
    """)
    suspend fun getNamaWithHargaTerakhir(tipe: TipeTransaksi): List<NamaDanHarga>

    /**
     * Autocomplete LINTAS KATEGORI: cari nama dari SELURUH histori (Barang maupun
     * Transaksi/Pulsa), tidak dibatasi tipe yang sedang dipilih di radio button.
     * Setiap hasil membawa tipe aslinya dari histori terakhir nama tersebut, supaya
     * saat dipilih, radio button bisa otomatis pindah mengikuti kategori itu.
     */
    @Query("""
        SELECT t.nama, t.harga, t.tipe
        FROM transaksi_items t
        INNER JOIN (
            SELECT nama, MAX(waktu) AS maxWaktu
            FROM transaksi_items
            GROUP BY nama
        ) latest ON t.nama = latest.nama AND t.waktu = latest.maxWaktu
        GROUP BY t.nama
        ORDER BY t.nama ASC
    """)
    suspend fun getNamaWithHargaTerakhirSemuaTipe(): List<NamaDanHarga>

    // ---------- Reset ----------
    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM transaksi_items")
    suspend fun deleteAllTransaksi()
}
