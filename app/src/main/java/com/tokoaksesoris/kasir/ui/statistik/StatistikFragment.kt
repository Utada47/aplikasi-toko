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

class StatistikFragment : Fragment() {

    private var _binding: FragmentStatistikBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatistikViewModel by viewModels {
        StatistikViewModel.Factory((requireActivity() as MainActivity).repository)
    }

    private val adapter = StatistikAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatistikBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvStatistik.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStatistik.adapter = adapter

        viewModel.data.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvKosong.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
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
