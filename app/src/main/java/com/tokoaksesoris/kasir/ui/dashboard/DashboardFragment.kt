package com.tokoaksesoris.kasir.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tokoaksesoris.kasir.MainActivity
import com.tokoaksesoris.kasir.R
import com.tokoaksesoris.kasir.databinding.FragmentDashboardBinding
import com.tokoaksesoris.kasir.utils.ThemeHelper
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory((requireActivity() as MainActivity).repository)
    }

    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))

    private lateinit var adapterBarang: TransaksiAdapter
    private lateinit var adapterPulsa: TransaksiAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapterBarang = TransaksiAdapter { item -> bukaDialogEdit(item) }
        adapterPulsa = TransaksiAdapter { item -> bukaDialogEdit(item) }
        binding.rvBarang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBarang.adapter = adapterBarang
        binding.rvPulsa.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPulsa.adapter = adapterPulsa

        binding.btnToggleTheme.setOnClickListener {
            ThemeHelper.toggleTheme(requireContext())
        }

        binding.btnMenuLainnya.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_export_csv -> { (requireActivity() as MainActivity).mulaiExportCsv(); true }
                    R.id.menu_import_csv -> { (requireActivity() as MainActivity).mulaiImportCsv(); true }
                    else -> false
                }
            }
            popup.show()
        }

        binding.btnMulaiHariIni.setOnClickListener {
            viewModel.mulaiHariIni()
        }

        binding.btnTutupHari.setOnClickListener {
            konfirmasiTutupHari()
        }

        binding.fabTambah.setOnClickListener {
            AddItemDialogFragment(itemToEdit = null) { tipe, nama, harga ->
                viewModel.tambahItem(tipe, nama, harga)
            }.show(childFragmentManager, "add_item")
        }

        viewModel.openSession.observe(viewLifecycleOwner) { session ->
            val adaSesi = session != null
            binding.btnMulaiHariIni.visibility = if (adaSesi) View.GONE else View.VISIBLE
            binding.groupSesiAktif.visibility = if (adaSesi) View.VISIBLE else View.GONE
            binding.fabTambah.visibility = if (adaSesi) View.VISIBLE else View.GONE
            binding.barTotalTetap.visibility = if (adaSesi) View.VISIBLE else View.GONE
        }

        viewModel.barangItems.observe(viewLifecycleOwner) { adapterBarang.submitList(it) }
        viewModel.pulsaItems.observe(viewLifecycleOwner) { adapterPulsa.submitList(it) }

        viewModel.totalBarang.observe(viewLifecycleOwner) { subtotal ->
            binding.tvSubtotalBarang.text = "Rp${rupiahFormat.format(subtotal)}"
        }
        viewModel.totalPulsa.observe(viewLifecycleOwner) { subtotal ->
            binding.tvSubtotalPulsa.text = "Rp${rupiahFormat.format(subtotal)}"
        }

        viewModel.total.observe(viewLifecycleOwner) { total ->
            binding.tvTotal.text = "Rp${rupiahFormat.format(total)}"
        }
    }

    private fun bukaDialogEdit(item: com.tokoaksesoris.kasir.data.TransaksiItem) {
        AddItemDialogFragment(itemToEdit = item) { tipe, nama, harga ->
            viewModel.updateItem(item, tipe, nama, harga)
        }.show(childFragmentManager, "edit_item")
    }

    private fun konfirmasiTutupHari() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Tutup Hari")
            .setMessage("Apakah Anda yakin ingin menutup hari ini? Setelah ditutup, item baru tidak bisa ditambahkan ke sesi ini lagi.")
            .setPositiveButton("Ya, Tutup") { _, _ -> viewModel.tutupHari() }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
