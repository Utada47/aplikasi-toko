package com.tokoaksesoris.kasir.ui.statistik

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tokoaksesoris.kasir.data.AppDatabase
import com.tokoaksesoris.kasir.data.Repository
import com.tokoaksesoris.kasir.data.TipeTransaksi
import com.tokoaksesoris.kasir.data.TransaksiItem
import com.tokoaksesoris.kasir.databinding.ActivityDetailHarianBinding
import com.tokoaksesoris.kasir.ui.dashboard.AddItemDialogFragment
import com.tokoaksesoris.kasir.ui.dashboard.TransaksiAdapter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetailHarianActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailHarianBinding

    private val repository: Repository by lazy {
        Repository(AppDatabase.getInstance(applicationContext).appDao())
    }

    private lateinit var adapterBarang: TransaksiAdapter
    private lateinit var adapterPulsa: TransaksiAdapter

    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))
    private val tanggalFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("in", "ID"))

    private var sessionIds: List<Long> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailHarianBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionIds = intent.getLongArrayExtra(EXTRA_SESSION_IDS)?.toList().orEmpty()
        val tanggalMulai = intent.getLongExtra(EXTRA_TANGGAL_MULAI, System.currentTimeMillis())
        binding.tvTanggalDetail.text = tanggalFormat.format(Date(tanggalMulai))

        binding.btnKembali.setOnClickListener { finish() }

        adapterBarang = TransaksiAdapter { item -> bukaDialogEdit(item) }
        adapterPulsa = TransaksiAdapter { item -> bukaDialogEdit(item) }
        binding.rvBarangDetail.layoutManager = LinearLayoutManager(this)
        binding.rvBarangDetail.adapter = adapterBarang
        binding.rvPulsaDetail.layoutManager = LinearLayoutManager(this)
        binding.rvPulsaDetail.adapter = adapterPulsa

        muatData()
    }

    private fun muatData() {
        lifecycleScope.launch {
            val semuaItem = repository.getAllTransaksi()
            val itemHariIni = semuaItem.filter { it.sessionId in sessionIds }
            val barang = itemHariIni.filter { it.tipe == TipeTransaksi.BARANG }.sortedBy { it.waktu }
            val pulsa = itemHariIni.filter { it.tipe == TipeTransaksi.PULSA }.sortedBy { it.waktu }

            adapterBarang.submitList(barang)
            adapterPulsa.submitList(pulsa)

            binding.tvKosongBarangDetail.visibility = if (barang.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvKosongPulsaDetail.visibility = if (pulsa.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE

            val totalBarang = barang.sumOf { it.harga }
            val totalPulsa = pulsa.sumOf { it.harga }
            binding.tvSubtotalBarangDetail.text = "Rp${rupiahFormat.format(totalBarang)}"
            binding.tvSubtotalPulsaDetail.text = "Rp${rupiahFormat.format(totalPulsa)}"
            binding.tvTotalDetail.text = "Rp${rupiahFormat.format(totalBarang + totalPulsa)}"
        }
    }

    private fun bukaDialogEdit(item: TransaksiItem) {
        AddItemDialogFragment(repository = repository, itemToEdit = item) { tipe, nama, harga ->
            lifecycleScope.launch {
                repository.updateItem(item.copy(tipe = tipe, nama = nama, harga = harga))
                muatData()
            }
        }.show(supportFragmentManager, "edit_item_detail")
    }

    companion object {
        private const val EXTRA_SESSION_IDS = "extra_session_ids"
        private const val EXTRA_TANGGAL_MULAI = "extra_tanggal_mulai"

        fun start(context: Context, sessionIds: List<Long>, tanggalMulai: Long) {
            val intent = Intent(context, DetailHarianActivity::class.java)
            intent.putExtra(EXTRA_SESSION_IDS, sessionIds.toLongArray())
            intent.putExtra(EXTRA_TANGGAL_MULAI, tanggalMulai)
            context.startActivity(intent)
        }
    }
}
