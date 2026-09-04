package com.tokoaksesoris.kasir.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

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

    @Query("SELECT * FROM transaksi_items WHERE sessionId = :sessionId AND tipe = :tipe ORDER BY waktu ASC")
    fun observeItemsBySessionAndTipe(sessionId: Long, tipe: TipeTransaksi): LiveData<List<TransaksiItem>>

    @Query("SELECT COALESCE(SUM(harga), 0.0) FROM transaksi_items WHERE sessionId = :sessionId")
    fun observeTotalBySession(sessionId: Long): LiveData<Double>

    @Query("SELECT COALESCE(SUM(harga), 0.0) FROM transaksi_items WHERE sessionId = :sessionId")
    suspend fun getTotalBySessionOnce(sessionId: Long): Double

    @Query("SELECT * FROM transaksi_items ORDER BY waktu ASC")
    suspend fun getAllTransaksiOnce(): List<TransaksiItem>

    // ---------- Migrasi / reset ----------
    @Query("DELETE FROM sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM transaksi_items")
    suspend fun deleteAllTransaksi()
}
