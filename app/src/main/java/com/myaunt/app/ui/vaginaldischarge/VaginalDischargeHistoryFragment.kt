package com.myaunt.app.ui.vaginaldischarge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.databinding.FragmentVaginalDischargeHistoryBinding

class VaginalDischargeHistoryFragment : Fragment() {

    private var _binding: FragmentVaginalDischargeHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PeriodRepository

    private lateinit var adapter: VaginalDischargeHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVaginalDischargeHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PeriodRepository(requireContext())

        adapter = VaginalDischargeHistoryAdapter(
            repository = repository,
            onItemClick = { record ->
                // 点击记录，可以跳转到编辑页面或显示详情
                // 这里暂时不做处理
            }
        )

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val records = repository.getAllVaginalDischargeRecords()
        
        if (records.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerHistory.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.recyclerHistory.visibility = View.VISIBLE
            adapter.submitList(records)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
