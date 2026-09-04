package com.tokoaksesoris.kasir.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tokoaksesoris.kasir.data.TransaksiItem
import com.tokoaksesoris.kasir.databinding.ItemTransaksiRowBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransaksiAdapter(
    private val onItemClick: (TransaksiItem) -> Unit
) : ListAdapter<TransaksiItem, TransaksiAdapter.VH>(DIFF) {

    private val jamFormat = SimpleDateFormat("HH:mm", Locale("in", "ID"))
    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))

    inner class VH(val binding: ItemTransaksiRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTransaksiRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.tvNo.text = (position + 1).toString()
        holder.binding.tvJam.text = jamFormat.format(Date(item.waktu))
        holder.binding.tvNama.text = item.nama
        holder.binding.tvHarga.text = "Rp${rupiahFormat.format(item.harga)}"
        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TransaksiItem>() {
            override fun areItemsTheSame(oldItem: TransaksiItem, newItem: TransaksiItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: TransaksiItem, newItem: TransaksiItem) = oldItem == newItem
        }
    }
}
