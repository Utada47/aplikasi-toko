package com.tokoaksesoris.kasir.data

class Repository(private val dao: AppDao) {

    suspend fun mulaiHariIni(): Session {
        dao.getOpenSession()?.let { return it }
        val now = System.currentTimeMillis()
        val session = Session(tanggalMulai = now, waktuMulai = now, isOpen = true)
        val id = dao.insertSession(session)
        return session.copy(id = id)
    }

    suspend fun tutupHari(session: Session) {
        dao.updateSession(session.copy(isOpen = false, waktuTutup = System.currentTimeMillis()))
    }

    suspend fun getOpenSession(): Session? = dao.getOpenSession()

    fun observeOpenSession() = dao.observeOpenSession()

    suspend fun tambahItem(sessionId: Long, tipe: TipeTransaksi, nama: String, harga: Double) {
        dao.insertTransaksi(
            TransaksiItem(sessionId = sessionId, tipe = tipe, nama = nama, harga = harga, waktu = System.currentTimeMillis())
        )
    }

    fun observeItems(sessionId: Long, tipe: TipeTransaksi) = dao.observeItemsBySessionAndTipe(sessionId, tipe)

    fun observeTotal(sessionId: Long) = dao.observeTotalBySession(sessionId)

    fun observeAllSessions() = dao.observeAllSessions()

    suspend fun getAllSessions() = dao.getAllSessionsOnce()

    suspend fun getTotalBySession(sessionId: Long) = dao.getTotalBySessionOnce(sessionId)

    suspend fun getAllTransaksi() = dao.getAllTransaksiOnce()

    // dipakai oleh CsvHelper saat import data lama
    suspend fun insertSessionRaw(session: Session) = dao.insertSession(session)
    suspend fun insertTransaksiRaw(item: TransaksiItem) = dao.insertTransaksi(item)

    suspend fun hapusSemuaData() {
        dao.deleteAllTransaksi()
        dao.deleteAllSessions()
    }
}
