package com.tokoaksesoris.kasir.ui.dashboard

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tokoaksesoris.kasir.data.TipeTransaksi
import com.tokoaksesoris.kasir.data.TransaksiItem
import com.tokoaksesoris.kasir.databinding.DialogAddItemBinding

/**
 * Modal tambah/edit item. Saat radio button diganti (Barang <-> Transaksi/Pulsa),
 * isi input (nama & harga) TIDAK direset -- hanya label hint yang berubah,
 * sesuai permintaan supaya user tidak perlu mengetik ulang.
 *
 * Jika [itemToEdit] diisi, dialog otomatis terisi data lama (mode edit).
 * Jika null, dialog kosong (mode tambah item baru).
 */
class AddItemDialogFragment(
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

        binding.radioGroupTipe.setOnCheckedChangeListener { _, checkedId ->
            val label = if (checkedId == binding.radioBarang.id) "Nama Barang" else "Nama Transaksi/Pulsa"
            binding.tilNama.hint = label
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

            val tipe = if (binding.radioGroupTipe.checkedRadioButtonId == binding.radioBarang.id)
                TipeTransaksi.BARANG else TipeTransaksi.PULSA

            onSimpan(tipe, nama, harga)
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
