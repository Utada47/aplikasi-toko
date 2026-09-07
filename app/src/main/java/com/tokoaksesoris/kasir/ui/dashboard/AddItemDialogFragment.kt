package com.tokoaksesoris.kasir.ui.dashboard

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tokoaksesoris.kasir.R
import com.tokoaksesoris.kasir.data.NamaDanHarga
import com.tokoaksesoris.kasir.data.Repository
import com.tokoaksesoris.kasir.data.TipeTransaksi
import com.tokoaksesoris.kasir.data.TransaksiItem
import com.tokoaksesoris.kasir.databinding.DialogAddItemBinding
import kotlinx.coroutines.launch

/**
 * Dialog tambah / edit item transaksi.
 *
 * Fitur autocomplete:
 * ─ Saran nama dicari LINTAS KATEGORI (Barang & Transaksi/Pulsa digabung) --
 *   tidak peduli radio button mana yang sedang aktif saat mengetik.
 * ─ Saat user memilih nama dari dropdown, field Harga otomatis terisi
 *   dengan harga terakhir yang pernah diinput untuk nama tersebut, DAN
 *   radio button otomatis pindah mengikuti kategori asli nama itu di histori
 *   (misal ketik "dana" lalu pilih "Top Up Dana" yang historinya Transaksi/Pulsa
 *   -> radio otomatis pindah ke Transaksi/Pulsa meski awalnya di Barang).
 * ─ Saat user menekan / menyentuh field Harga (yang sudah terisi otomatis),
 *   isi langsung dikosongkan — user tidak perlu hapus manual terlebih dahulu.
 * ─ Jika user mengetik nama secara manual (tidak pilih dari dropdown),
 *   field Harga & radio button tidak tersentuh sama sekali.
 */
class AddItemDialogFragment(
    private val repository: Repository,
    private val itemToEdit: TransaksiItem? = null,
    private val onSimpan: (tipe: TipeTransaksi, nama: String, harga: Double) -> Unit
) : DialogFragment() {

    private var _binding: DialogAddItemBinding? = null
    private val binding get() = _binding!!

    /**
     * true  → harga field terisi DARI hasil autocomplete (bukan ketik manual).
     *          Artinya: saat field ditekan → kosongkan otomatis.
     * false → harga field kosong atau diisi manual → tidak perlu dikosongkan.
     */
    private var hargaDariAutocomplete = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddItemBinding.inflate(layoutInflater)

        // ── Mode edit: isi data lama ─────────────────────────────────────────
        if (itemToEdit != null) {
            binding.tvJudulDialog.text = "Edit Item"
            binding.btnMasukkan.text  = "Simpan Perubahan"
            binding.etNama.setText(itemToEdit.nama)
            binding.etHarga.setText(itemToEdit.harga.toLong().toString())
            if (itemToEdit.tipe == TipeTransaksi.PULSA) {
                binding.toggleTipe.check(binding.btnTipePulsa.id)
            } else {
                binding.toggleTipe.check(binding.btnTipeBarang.id)
            }
        }

        // ── Muat saran autocomplete (lintas kategori, sekali saja) ───────────
        muatSaranGlobal()

        // ── Ganti tipe manual (tap tab langsung) → hint berubah saja ─────────
        // Saran TIDAK perlu dimuat ulang karena sudah lintas kategori dari awal.
        binding.toggleTipe.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            binding.tilNama.hint = if (checkedId == binding.btnTipeBarang.id)
                "Nama Barang" else "Nama Transaksi / Pulsa"
            // etNama & etHarga TIDAK di-clear saat ganti tipe (sesuai spesifikasi)
        }

        // ── Saat item autocomplete dipilih dari dropdown ──────────────────────
        binding.etNama.setOnItemClickListener { adapterView, _, position, _ ->
            val dipilih = adapterView.getItemAtPosition(position) as? NamaDanHarga
                ?: return@setOnItemClickListener

            // Isi harga otomatis dari harga terakhir
            val hargaBulat = dipilih.harga.toLong().toString()
            binding.etHarga.setText(hargaBulat)
            binding.tilHarga.hint = "Harga (terisi otomatis — ketuk untuk ubah)"
            hargaDariAutocomplete = true

            // Tab otomatis ikut kategori asli nama ini di histori
            if (dipilih.tipe == TipeTransaksi.PULSA) {
                binding.toggleTipe.check(binding.btnTipePulsa.id)
            } else {
                binding.toggleTipe.check(binding.btnTipeBarang.id)
            }

            // Bersihkan error jika ada
            binding.tilNama.error  = null
            binding.tilHarga.error = null
        }

        // ── Saat field Harga ditekan / difokus ───────────────────────────────
        // Jika harga berasal dari autocomplete → kosongkan seketika
        // sehingga user tidak perlu hapus manual.
        binding.etHarga.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && hargaDariAutocomplete) {
                kosongkanHarga()
            }
        }

        // setOnClickListener sebagai fallback: menangani kasus ketika
        // field sudah dalam keadaan fokus lalu ditekan lagi.
        binding.etHarga.setOnClickListener {
            if (hargaDariAutocomplete) {
                kosongkanHarga()
            }
        }

        // ── Tombol Masukkan / Simpan ─────────────────────────────────────────
        binding.btnMasukkan.setOnClickListener {
            val nama  = binding.etNama.text?.toString()?.trim().orEmpty()
            val hargaStr = binding.etHarga.text?.toString()?.trim().orEmpty()
            val harga = hargaStr.toDoubleOrNull()

            var valid = true
            if (nama.isEmpty()) {
                binding.tilNama.error = "Wajib diisi"; valid = false
            } else {
                binding.tilNama.error = null
            }
            if (harga == null || harga <= 0) {
                binding.tilHarga.error = "Harga tidak valid"; valid = false
            } else {
                binding.tilHarga.error = null
            }
            if (!valid) return@setOnClickListener

            onSimpan(tipeTerpilih(), nama, harga!!)
            dismiss()
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    // ── Helper: kosongkan harga dan reset state ───────────────────────────────
    private fun kosongkanHarga() {
        binding.etHarga.text?.clear()
        binding.tilHarga.hint = "Harga"
        hargaDariAutocomplete = false
    }

    // ── Helper: tipe yang sedang dipilih ─────────────────────────────────────
    private fun tipeTerpilih(): TipeTransaksi =
        if (binding.toggleTipe.checkedButtonId == binding.btnTipeBarang.id)
            TipeTransaksi.BARANG else TipeTransaksi.PULSA

    // ── Muat saran autocomplete dari DB (lintas kategori) ────────────────────
    private fun muatSaranGlobal() {
        lifecycleScope.launch {
            val daftarSaran = repository.getSaranNamaSemuaTipe()
            if (_binding == null) return@launch   // dialog sudah ditutup

            // ArrayAdapter<NamaDanHarga> — toString() pada NamaDanHarga
            // mengembalikan hanya nama, sehingga dropdown hanya tampilkan nama.
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown_saran,
                daftarSaran
            )
            binding.etNama.setAdapter(adapter)
            binding.etNama.threshold = 1   // mulai saran setelah 1 karakter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
