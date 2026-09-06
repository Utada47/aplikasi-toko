package com.tokoaksesoris.kasir.ui.statistik

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tokoaksesoris.kasir.databinding.ItemStatistikRowBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatistikAdapter(
    private val onItemClick: (StatistikHarian) -> Unit
) : ListAdapter<StatistikHarian, StatistikAdapter.VH>(DIFF) {

    private val tanggalFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("in", "ID"))
    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))

    inner class VH(val binding: ItemStatistikRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStatistikRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.tvTanggal.text = tanggalFormat.format(Date(item.tanggalMulai))
        holder.binding.tvTotalHarian.text = "Rp${rupiahFormat.format(item.total)}"
        holder.binding.tvStatus.text = "${item.jumlahItem} item total · ${item.statusText}"
        holder.binding.tvJumlahBarang.text = "${item.jumlahBarang} item"
        holder.binding.tvSubtotalBarangHarian.text = "Rp${rupiahFormat.format(item.totalBarang)}"
        holder.binding.tvJumlahPulsa.text = "${item.jumlahPulsa} item"
        holder.binding.tvSubtotalPulsaHarian.text = "Rp${rupiahFormat.format(item.totalPulsa)}"
        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StatistikHarian>() {
            override fun areItemsTheSame(oldItem: StatistikHarian, newItem: StatistikHarian) =
                oldItem.sessionId == newItem.sessionId
            override fun areContentsTheSame(oldItem: StatistikHarian, newItem: StatistikHarian) = oldItem == newItem
        }
    }
}
