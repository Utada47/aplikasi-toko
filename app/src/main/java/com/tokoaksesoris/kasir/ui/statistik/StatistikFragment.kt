package com.tokoaksesoris.kasir.ui.statistik

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tokoaksesoris.kasir.MainActivity
import com.tokoaksesoris.kasir.databinding.FragmentStatistikBinding
import java.text.NumberFormat
import java.util.Locale

class StatistikFragment : Fragment() {

    private var _binding: FragmentStatistikBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatistikViewModel by viewModels {
        StatistikViewModel.Factory((requireActivity() as MainActivity).repository)
    }

    private val adapterHarian = StatistikAdapter { item ->
        DetailHarianActivity.start(requireContext(), item.sessionIds, item.tanggalMulai)
    }
    private val adapterTerlaris = BarangTerlarisAdapter()

    private val rupiahFormat = NumberFormat.getNumberInstance(Locale("in", "ID"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatistikBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvStatistik.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStatistik.adapter = adapterHarian

        binding.rvBarangTerlaris.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBarangTerlaris.adapter = adapterTerlaris

        binding.toggleRentang.check(binding.btnRentang7Hari.id)
        binding.toggleRentang.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val rentang = when (checkedId) {
                binding.btnRentang30Hari.id -> RentangWaktu.TIGA_PULUH_HARI
                binding.btnRentangSemua.id -> RentangWaktu.SEMUA
                else -> RentangWaktu.TUJUH_HARI
            }
            viewModel.setRentang(rentang)
        }

        viewModel.data.observe(viewLifecycleOwner) { list ->
            adapterHarian.submitList(list)
            binding.tvKosong.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.insight.observe(viewLifecycleOwner) { insight ->
            binding.tvInsightHariIni.text = "Rp${rupiahFormat.format(insight.totalHariIni)}"
            binding.tvInsightKemarin.text = "Rp${rupiahFormat.format(insight.totalKemarin)}"
            binding.tvInsightRataRata.text = "Rp${rupiahFormat.format(insight.rataRata7Hari)}"
        }

        viewModel.chartData.observe(viewLifecycleOwner) { entries ->
            binding.barChart.setData(entries)
        }

        viewModel.barangTerlaris.observe(viewLifecycleOwner) { list ->
            adapterTerlaris.submitList(list)
            binding.tvKosongTerlaris.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBarangTerlaris.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.muatUlang()
    }

    override fun onResume() {
        super.onResume()
        viewModel.muatUlang() // refresh setiap kali tab statistik dibuka
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
