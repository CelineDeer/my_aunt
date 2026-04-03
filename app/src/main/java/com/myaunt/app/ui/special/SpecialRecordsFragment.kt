package com.myaunt.app.ui.special

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.myaunt.app.R
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.ui.applyStatusBarPaddingTop
import com.myaunt.app.data.SpecialRecord
import com.myaunt.app.databinding.FragmentSpecialRecordsBinding
import java.time.format.DateTimeFormatter
import java.util.Locale

class SpecialRecordsFragment : Fragment() {

    private val dialogDateFormat = DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)

    private var _binding: FragmentSpecialRecordsBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PeriodRepository

    private val adapter = SpecialRecordsAdapter(
        onEdit = { showEditDialog(it) },
        onDelete = { showDeleteConfirm(it) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSpecialRecordsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PeriodRepository(requireContext())
        binding.root.applyStatusBarPaddingTop()
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun refreshList() {
        val list = repository.getSpecialRecords()
        adapter.submitList(list)
        val empty = list.isEmpty()
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recycler.visibility = if (empty) View.GONE else View.VISIBLE

        binding.tvSummaryBody.text = if (empty) {
            getString(R.string.special_history_summary_empty)
        } else {
            val avg = repository.getAverageCycle()
            if (avg != null) {
                getString(R.string.special_history_summary_with_avg, list.size, avg)
            } else {
                getString(R.string.special_history_summary_count_only, list.size)
            }
        }
    }

    private fun showEditDialog(record: SpecialRecord) {
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = "备注内容"
            setPadding(48, 24, 48, 0)
        }
        val input = TextInputEditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(record.reason)
        }
        inputLayout.addView(input)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("编辑备注 · ${record.date.format(dialogDateFormat)}")
            .setView(inputLayout)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val text = input.text?.toString()?.trim().orEmpty()
                if (text.isEmpty()) {
                    inputLayout.error = "内容不能为空"
                    return@setOnClickListener
                }
                inputLayout.error = null
                repository.updateSpecialReason(record.date, text)
                dialog.dismiss()
                refreshList()
            }
        }
        dialog.show()
    }

    private fun showDeleteConfirm(record: SpecialRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除备注？")
            .setMessage("只会删除这条文字说明，月经记录日期仍会保留。")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                repository.removeSpecialReason(record.date)
                refreshList()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
