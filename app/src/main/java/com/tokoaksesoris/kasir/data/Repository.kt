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
    fun observeAllSessions() = dao.observeAllSessions()
    suspend fun getAllSessions() = dao.getAllSessionsOnce()

    suspend fun tambahItem(sessionId: Long, tipe: TipeTransaksi, nama: String, harga: Double) {
        dao.insertTransaksi(
            TransaksiItem(
                sessionId = sessionId,
                tipe      = tipe,
                nama      = nama,
                harga     = harga,
                waktu     = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateItem(item: TransaksiItem) = dao.updateTransaksi(item)
    suspend fun hapusItem(item: TransaksiItem)  = dao.deleteTransaksi(item)

    suspend fun kembalikanItem(item: TransaksiItem) {
        dao.insertTransaksi(item.copy(id = 0))
    }

    /**
     * Kembalikan daftar saran autocomplete:
     * masing-masing berisi nama + harga terakhir yang pernah diinput
     * untuk tipe tersebut.
     */
    suspend fun getSaranNamaWithHarga(tipe: TipeTransaksi): List<NamaDanHarga> =
        dao.getNamaWithHargaTerakhir(tipe)

    /** Kompatibilitas mundur — hanya nama tanpa harga. */
    suspend fun getSaranNama(tipe: TipeTransaksi): List<String> =
        dao.getNamaWithHargaTerakhir(tipe).map { it.nama }

    fun observeItems(sessionId: Long, tipe: TipeTransaksi) =
        dao.observeItemsBySessionAndTipe(sessionId, tipe)

    fun observeTotal(sessionId: Long) = dao.observeTotalBySession(sessionId)

    suspend fun getTotalBySession(sessionId: Long) = dao.getTotalBySessionOnce(sessionId)
    suspend fun getAllTransaksi() = dao.getAllTransaksiOnce()

    suspend fun insertSessionRaw(session: Session)    = dao.insertSession(session)
    suspend fun insertTransaksiRaw(item: TransaksiItem) = dao.insertTransaksi(item)

    suspend fun hapusSemuaData() {
        dao.deleteAllTransaksi()
        dao.deleteAllSessions()
    }
}
