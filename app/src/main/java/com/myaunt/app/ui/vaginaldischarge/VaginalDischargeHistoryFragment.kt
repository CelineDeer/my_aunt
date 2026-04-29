package com.myaunt.app.ui.vaginaldischarge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.databinding.FragmentVaginalDischargeHistoryBinding
import com.myaunt.app.ui.recordbook.RecordBookFragment

class VaginalDischargeHistoryFragment : Fragment() {

    companion object {
        private const val ARG_EMBEDDED = "embedded"

        fun newInstance(embedded: Boolean = false) = VaginalDischargeHistoryFragment().apply {
            arguments = bundleOf(ARG_EMBEDDED to embedded)
        }
    }

    private var _binding: FragmentVaginalDischargeHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PeriodRepository
    private val embedded: Boolean by lazy { arguments?.getBoolean(ARG_EMBEDDED) == true }

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
                (parentFragment as? RecordBookFragment)?.openDischargeAtDate(record.date)
            },
        )

        if (embedded) {
            binding.toolbar.navigationIcon = null
        } else {
            binding.toolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
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

    /** 由记录本在保存后切换 Tab 时调用，确保立即反映新数据。 */
    fun refreshHistoryList() {
        if (!isAdded || _binding == null) return
        loadHistory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
