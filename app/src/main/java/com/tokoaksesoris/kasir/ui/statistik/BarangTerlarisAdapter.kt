package com.tokoaksesoris.kasir.ui.statistik

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tokoaksesoris.kasir.databinding.ItemBarangTerlarisRowBinding
import java.text.NumberFormat
import java.util.Locale

class BarangTerlarisAdapter : ListAdapter<BarangTerlaris, BarangTerlarisAdapter.VH>(DIFF) {

    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))

    inner class VH(val binding: ItemBarangTerlarisRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBarangTerlarisRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.tvPeringkat.text = "${position + 1}"
        holder.binding.tvNamaBarang.text = item.nama
        holder.binding.tvJumlahTerjual.text = "${item.jumlahTerjual}x"
        holder.binding.tvPendapatanBarang.text = "Rp${rupiahFormat.format(item.totalPendapatan)}"
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BarangTerlaris>() {
            override fun areItemsTheSame(oldItem: BarangTerlaris, newItem: BarangTerlaris) =
                oldItem.nama == newItem.nama && oldItem.tipe == newItem.tipe
            override fun areContentsTheSame(oldItem: BarangTerlaris, newItem: BarangTerlaris) = oldItem == newItem
        }
    }
}
