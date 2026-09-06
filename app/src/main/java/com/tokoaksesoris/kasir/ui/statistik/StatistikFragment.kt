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
        val modeSekarang = viewModel.mode.value ?: ModeTampilan.HARIAN
        val judulKustom = if (modeSekarang == ModeTampilan.HARIAN) null else item.label
        DetailHarianActivity.start(requireContext(), item.sessionIds, item.representativeTimestamp, judulKustom)
    }

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

        binding.toggleMode.check(binding.btnModeHarian.id)
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val modePilihan = when (checkedId) {
                binding.btnModeBulanan.id -> ModeTampilan.BULANAN
                binding.btnModeTahunan.id -> ModeTampilan.TAHUNAN
                else -> ModeTampilan.HARIAN
            }
            val (labelKecil, labelBesar) = viewModel.labelRentangUntukMode(modePilihan)
            binding.btnRentangKecil.text = labelKecil
            binding.btnRentangBesar.text = labelBesar
            binding.toggleRentang.check(binding.btnRentangKecil.id) // reset ke opsi pertama tiap ganti mode
            binding.tvLabelRiwayat.text = when (modePilihan) {
                ModeTampilan.HARIAN -> "RIWAYAT HARIAN"
                ModeTampilan.BULANAN -> "RIWAYAT BULANAN"
                ModeTampilan.TAHUNAN -> "RIWAYAT TAHUNAN"
            }
            viewModel.setMode(modePilihan)
        }

        binding.toggleRentang.check(binding.btnRentangKecil.id)
        binding.toggleRentang.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val index = when (checkedId) {
                binding.btnRentangBesar.id -> 1
                binding.btnRentangSemua.id -> 2
                else -> 0
            }
            viewModel.setRentangIndex(index)
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
