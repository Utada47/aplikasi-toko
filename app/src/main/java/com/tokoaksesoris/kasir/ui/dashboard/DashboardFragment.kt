package com.tokoaksesoris.kasir.ui.dashboard

import android.content.ClipData
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tokoaksesoris.kasir.MainActivity
import com.tokoaksesoris.kasir.R
import com.tokoaksesoris.kasir.data.TipeTransaksi
import com.tokoaksesoris.kasir.data.TransaksiItem
import com.tokoaksesoris.kasir.databinding.FragmentDashboardBinding
import com.tokoaksesoris.kasir.utils.ThemeHelper
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

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

        adapterBarang = TransaksiAdapter(
            onItemClick = { item -> bukaDialogEdit(item) },
            onLongPress = { item, view -> mulaiDrag(item, view) }
        )
        adapterPulsa = TransaksiAdapter(
            onItemClick = { item -> bukaDialogEdit(item) },
            onLongPress = { item, view -> mulaiDrag(item, view) }
        )
        binding.rvBarang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBarang.adapter = adapterBarang
        binding.rvPulsa.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPulsa.adapter = adapterPulsa

        pasangSwipeToDelete(binding.rvBarang, adapterBarang)
        pasangSwipeToDelete(binding.rvPulsa, adapterPulsa)

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
            AddItemDialogFragment(repository = (requireActivity() as MainActivity).repository, itemToEdit = null) { tipe, nama, harga ->
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

    /**
     * Pasang gesture swipe (geser kiri) pada RecyclerView untuk menghapus item.
     * Menampilkan latar merah + ikon tempat sampah saat digeser, lalu modal konfirmasi
     * sebelum benar-benar menghapus. Kalau dibatalkan, baris otomatis kembali ke posisi semula.
     * Setelah dihapus, tetap ada Snackbar "Undo" sebagai jaring pengaman tambahan.
     */
    private fun pasangSwipeToDelete(recyclerView: RecyclerView, adapter: TransaksiAdapter) {
        val background = ColorDrawable(Color.parseColor("#FF3B30")) // iOS system red
        val ikonHapus = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)

        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return
                val item = adapter.currentList[position]

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Hapus Item")
                    .setMessage("Hapus \"${item.nama}\" (Rp${rupiahFormat.format(item.harga)}) dari daftar?")
                    .setPositiveButton("Hapus") { _, _ ->
                        viewModel.hapusItem(item)
                        Snackbar.make(binding.root, "Item \"${item.nama}\" dihapus", Snackbar.LENGTH_LONG)
                            .setAction("Undo") { viewModel.undoHapus(item) }
                            .show()
                    }
                    .setNegativeButton("Batal") { _, _ ->
                        adapter.notifyItemChanged(position) // kembalikan baris ke posisi semula
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position) // ditutup dgn back/tap luar -> tetap kembalikan
                    }
                    .show()
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                if (dX < 0) {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)

                    ikonHapus?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = min(itemView.right - iconMargin - it.intrinsicWidth, itemView.right - 16)
                        val iconRight = itemView.right - iconMargin
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, rv, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun bukaDialogEdit(item: TransaksiItem) {
        val repo = (requireActivity() as MainActivity).repository
        AddItemDialogFragment(repository = repo, itemToEdit = item) { tipe, nama, harga ->
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
