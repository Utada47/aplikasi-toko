package com.tokoaksesoris.kasir.ui.dashboard

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tokoaksesoris.kasir.R
import com.tokoaksesoris.kasir.data.Repository
import com.tokoaksesoris.kasir.data.TipeTransaksi
import com.tokoaksesoris.kasir.data.TransaksiItem
import com.tokoaksesoris.kasir.databinding.DialogAddItemBinding
import kotlinx.coroutines.launch

/**
 * Modal tambah/edit item. Saat radio button diganti (Barang <-> Transaksi/Pulsa),
 * isi input (nama & harga) TIDAK direset -- hanya label hint & saran autocomplete
 * yang berubah, sesuai permintaan supaya user tidak perlu mengetik ulang.
 *
 * Jika [itemToEdit] diisi, dialog otomatis terisi data lama (mode edit).
 * Jika null, dialog kosong (mode tambah item baru).
 *
 * [repository] di-pass langsung (bukan diambil dari activity) supaya dialog ini
 * bisa dipakai dari Activity mana pun -- Dashboard maupun DetailHarianActivity.
 */
class AddItemDialogFragment(
    private val repository: Repository,
    private val itemToEdit: TransaksiItem? = null,
    private val onSimpan: (tipe: TipeTransaksi, nama: String, harga: Double) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddItemBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddItemBinding.inflate(layoutInflater)

        val sedangEdit = itemToEdit != null

        if (sedangEdit) {
            binding.tvJudulDialog.text = "Edit Item"
            binding.btnMasukkan.text = "Simpan Perubahan"
            binding.etNama.setText(itemToEdit!!.nama)
            binding.etHarga.setText(itemToEdit.harga.toLong().toString())
            if (itemToEdit.tipe == TipeTransaksi.PULSA) {
                binding.radioPulsa.isChecked = true
            } else {
                binding.radioBarang.isChecked = true
            }
        }

        muatSaranNama(tipeTerpilih())

        binding.radioGroupTipe.setOnCheckedChangeListener { _, checkedId ->
            val label = if (checkedId == binding.radioBarang.id) "Nama Barang" else "Nama Transaksi/Pulsa"
            binding.tilNama.hint = label
            muatSaranNama(tipeTerpilih())
            // catatan: etNama & etHarga TIDAK di-clear, sesuai spesifikasi
        }

        binding.btnMasukkan.setOnClickListener {
            val nama = binding.etNama.text?.toString()?.trim().orEmpty()
            val hargaText = binding.etHarga.text?.toString()?.trim().orEmpty()
            val harga = hargaText.toDoubleOrNull()

            if (nama.isEmpty()) {
                binding.tilNama.error = "Wajib diisi"
                return@setOnClickListener
            }
            if (harga == null || harga <= 0) {
                binding.tilHarga.error = "Harga tidak valid"
                return@setOnClickListener
            }

            onSimpan(tipeTerpilih(), nama, harga)
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun tipeTerpilih(): TipeTransaksi =
        if (binding.radioGroupTipe.checkedRadioButtonId == binding.radioBarang.id)
            TipeTransaksi.BARANG else TipeTransaksi.PULSA

    private fun muatSaranNama(tipe: TipeTransaksi) {
        lifecycleScope.launch {
            val saran = repository.getSaranNama(tipe)
            if (_binding == null) return@launch // dialog mungkin sudah ditutup
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_saran, saran)
            binding.etNama.setAdapter(adapter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
