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
     * Autocomplete: kembalikan satu baris per nama unik,
     * beserta HARGA TERAKHIR (waktu terbesar) untuk nama tersebut.
     *
     * Cara kerja query:
     *  - subquery mencari waktu MAX per nama dalam tipe yang sama
     *  - join ke tabel utama untuk ambil harga pada waktu tersebut
     *  - GROUP BY nama memastikan hanya satu baris per nama
     *    (menghindari duplikat jika ada dua item dengan waktu sama persis)
     */
    @Query("""
        SELECT t.nama, t.harga
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

    // ---------- Reset ----------
    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM transaksi_items")
    suspend fun deleteAllTransaksi()
}
