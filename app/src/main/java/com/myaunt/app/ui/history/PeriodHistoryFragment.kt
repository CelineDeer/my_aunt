package com.myaunt.app.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.myaunt.app.R
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.databinding.FragmentPeriodHistoryBinding
import com.myaunt.app.databinding.ItemPeriodHistoryBinding
import com.myaunt.app.ui.applyStatusBarPaddingTop
import com.myaunt.app.widget.PeriodWidgetUpdater
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private data class PeriodHistoryRow(
    val date: LocalDate,
    /** 与**更早**一条月经开始日之间的间隔（天），最早一条为 null。 */
    val daysToOlder: Int?,
    val hasSpecialNote: Boolean,
    val isEditable: Boolean,
)

class PeriodHistoryFragment : Fragment() {

    private var _binding: FragmentPeriodHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: PeriodRepository
    private lateinit var adapter: PeriodHistoryAdapter
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINESE)
    private var editDate: LocalDate? = null

    companion object {
        private const val ARG_EDIT_DATE = "edit_date"

        fun newInstanceForEdit(date: LocalDate): PeriodHistoryFragment {
            return PeriodHistoryFragment().apply {
                arguments = bundleOf(ARG_EDIT_DATE to date.toString())
            }
        }
    }

    /** 由 MainActivity 调用，高亮并开放某条记录的编辑入口。 */
    fun prepareEditMode(date: LocalDate) {
        editDate = date
        if (_binding != null) loadList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPeriodHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PeriodRepository(requireContext())
        binding.root.applyStatusBarPaddingTop()
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = PeriodHistoryAdapter(
            dateFormatter = dateFormatter,
            onDelete = { date -> showDeleteDialog(date) },
            onEdit = { date ->
                prepareEditMode(date)
                loadList()
            },
        )
        binding.recyclerPeriods.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPeriods.adapter = adapter
        arguments?.getString(ARG_EDIT_DATE)?.let { editDate = LocalDate.parse(it) }
        loadList()
    }

    override fun onResume() {
        super.onResume()
        loadList()
    }

    private fun loadList() {
        val periods = repository.getAllPeriods()
        if (periods.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.recyclerPeriods.visibility = View.GONE
            return
        }
        binding.tvEmpty.visibility = View.GONE
        binding.recyclerPeriods.visibility = View.VISIBLE
        val rows = periods.mapIndexed { i, date ->
            val older = periods.getOrNull(i + 1)
            val gap = older?.let { ChronoUnit.DAYS.between(it, date).toInt() }
            PeriodHistoryRow(
                date = date,
                daysToOlder = gap,
                hasSpecialNote = repository.getSpecialReason(date) != null,
                isEditable = date == editDate
            )
        }
        adapter.submitList(rows)
    }

    private fun showDeleteDialog(date: LocalDate) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.period_history_delete_title)
            .setMessage(R.string.period_history_delete_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.period_history_delete_confirm) { _, _ ->
                val oldReason = repository.getSpecialReason(date)
                repository.removePeriod(date)
                PeriodWidgetUpdater.updateAll(requireContext())
                loadList()
                showUndoSnackbar(date, oldReason)
            }
            .show()
    }

    private fun showUndoSnackbar(date: LocalDate, oldReason: String?) {
        Snackbar.make(binding.root, R.string.period_history_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_undo) {
                restoreDeletedPeriod(date, oldReason)
            }
            .show()
    }

    private fun restoreDeletedPeriod(date: LocalDate, oldReason: String?) {
        if (oldReason.isNullOrBlank()) {
            repository.addPeriod(date)
        } else {
            repository.addPeriodWithSpecialReason(date, oldReason)
        }
        PeriodWidgetUpdater.updateAll(requireContext())
        loadList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class PeriodHistoryAdapter(
        private val dateFormatter: DateTimeFormatter,
        private val onDelete: (LocalDate) -> Unit,
        private val onEdit: (LocalDate) -> Unit,
    ) : RecyclerView.Adapter<PeriodHistoryAdapter.VH>() {

        private var items: List<PeriodHistoryRow> = emptyList()

        fun submitList(rows: List<PeriodHistoryRow>) {
            items = rows
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val inflater = LayoutInflater.from(parent.context)
            val itemBinding = ItemPeriodHistoryBinding.inflate(inflater, parent, false)
            return VH(itemBinding, dateFormatter, onDelete, onEdit)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class VH(
            private val itemBinding: ItemPeriodHistoryBinding,
            private val dateFormatter: DateTimeFormatter,
            private val onDelete: (LocalDate) -> Unit,
            private val onEdit: (LocalDate) -> Unit,
        ) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(row: PeriodHistoryRow) {
                val isEditable = row.isEditable
                itemBinding.tvDate.text = row.date.format(dateFormatter)
                if (row.daysToOlder != null) {
                    itemBinding.tvInterval.visibility = View.VISIBLE
                    itemBinding.tvInterval.text = itemView.context.getString(
                        R.string.period_history_interval,
                        row.daysToOlder,
                    )
                } else {
                    itemBinding.tvInterval.visibility = View.GONE
                }
                itemBinding.tvNoteHint.visibility =
                    if (row.hasSpecialNote) View.VISIBLE else View.GONE
                if (isEditable) {
                    itemBinding.btnEdit.visibility = View.VISIBLE
                    itemBinding.btnDelete.visibility = View.GONE
                    itemBinding.btnEdit.setOnClickListener { onEdit(row.date) }
                } else {
                    itemBinding.btnEdit.visibility = View.GONE
                    itemBinding.btnDelete.visibility = View.VISIBLE
                    itemBinding.btnDelete.setOnClickListener { onDelete(row.date) }
                }
            }
        }
    }
}
