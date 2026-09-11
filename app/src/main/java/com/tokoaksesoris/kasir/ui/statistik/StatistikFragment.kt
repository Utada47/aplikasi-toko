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

    /** Jarak (px) dari dasar konten yang memicu "muat lebih banyak" saat discroll mendekati bawah. */
    private val ambangBatasScrollPx by lazy { (resources.displayMetrics.density * 300).toInt() }

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
        binding.rvStatistik.isNestedScrollingEnabled = false

        binding.toggleMode.check(binding.btnModeHarian.id)
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val modePilihan = when (checkedId) {
                binding.btnModeBulanan.id -> ModeTampilan.BULANAN
                binding.btnModeTahunan.id -> ModeTampilan.TAHUNAN
                else -> ModeTampilan.HARIAN
            }
            binding.tvLabelRiwayat.text = when (modePilihan) {
                ModeTampilan.HARIAN -> "RIWAYAT HARIAN"
                ModeTampilan.BULANAN -> "RIWAYAT BULANAN"
                ModeTampilan.TAHUNAN -> "RIWAYAT TAHUNAN"
            }
            // Scroll balik ke atas dulu supaya user melihat halaman pertama dari mode barunya.
            binding.scrollStatistik.smoothScrollTo(0, 0)
            viewModel.setMode(modePilihan)
        }

        // ── Infinite scroll: pantau scroll pada NestedScrollView pembungkus,
        //    karena RecyclerView-nya sendiri tidak scroll independen (nestedScrollingEnabled=false).
        binding.scrollStatistik.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            val kontenUtama = v.getChildAt(0) ?: return@setOnScrollChangeListener
            val sudahDekatBawah = (scrollY + v.height) >= (kontenUtama.height - ambangBatasScrollPx)
            if (sudahDekatBawah) {
                viewModel.muatLebihBanyak()
            }
        }

        viewModel.data.observe(viewLifecycleOwner) { list ->
            adapterHarian.submitList(list)
            binding.tvKosong.visibility = if (list.isEmpty() && viewModel.sedangMuat.value != true) View.VISIBLE else View.GONE
        }

        viewModel.sedangMuatLebih.observe(viewLifecycleOwner) { sedangMuat ->
            binding.loadingLebihBanyak.visibility = if (sedangMuat) View.VISIBLE else View.GONE
        }

        viewModel.adaLebihBanyak.observe(viewLifecycleOwner) { adaLagi ->
            val adaData = !viewModel.data.value.isNullOrEmpty()
            binding.tvSemuaTertampil.visibility = if (!adaLagi && adaData) View.VISIBLE else View.GONE
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
        viewModel.muatUlang() // refresh dari awal setiap kali tab statistik dibuka
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
