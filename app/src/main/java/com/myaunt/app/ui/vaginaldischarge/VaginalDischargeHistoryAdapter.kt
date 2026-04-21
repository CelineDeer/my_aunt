package com.myaunt.app.ui.vaginaldischarge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.data.VaginalDischargeRecord
import com.myaunt.app.databinding.ItemVaginalDischargeHistoryBinding
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class VaginalDischargeHistoryAdapter(
    private val repository: PeriodRepository,
    private val onItemClick: (VaginalDischargeRecord) -> Unit
) : RecyclerView.Adapter<VaginalDischargeHistoryAdapter.ViewHolder>() {

    private var records: List<VaginalDischargeRecord> = emptyList()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINESE)

    fun submitList(newRecords: List<VaginalDischargeRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVaginalDischargeHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    inner class ViewHolder(
        private val binding: ItemVaginalDischargeHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: VaginalDischargeRecord) {
            binding.tvDate.text = record.date.format(dateFormatter)
            
            // 计算周期天数
            val cycleDay = calculateCycleDay(record.date)
            if (cycleDay != null) {
                binding.tvCycleDay.text = "周期第${cycleDay}天"
                binding.tvCycleDay.visibility = android.view.View.VISIBLE
            } else {
                binding.tvCycleDay.visibility = android.view.View.GONE
            }

            binding.tvAmount.text = record.amount.displayName
            binding.tvColor.text = record.color.displayName
            binding.tvTexture.text = record.texture.displayName
            binding.tvSensation.text = record.sensation.displayName

            // 标签
            val hasTags = record.abdominalDiscomfort || record.afterIntercourse
            if (hasTags) {
                binding.layoutTags.visibility = android.view.View.VISIBLE
                binding.chipAbdominal.visibility = if (record.abdominalDiscomfort) android.view.View.VISIBLE else android.view.View.GONE
                binding.chipAfterIntercourse.visibility = if (record.afterIntercourse) android.view.View.VISIBLE else android.view.View.GONE
            } else {
                binding.layoutTags.visibility = android.view.View.GONE
            }

            // 备注
            if (record.notes.isNotEmpty()) {
                binding.tvNotes.text = record.notes
                binding.tvNotes.visibility = android.view.View.VISIBLE
            } else {
                binding.tvNotes.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(record)
            }
        }

        private fun calculateCycleDay(date: java.time.LocalDate): Int? {
            val periods = repository.getAllPeriods()
            if (periods.isEmpty()) return null
            
            // 找到该日期之前最近的一次月经开始日
            val lastPeriod = periods.filter { it.isBefore(date) || it.isEqual(date) }.maxOrNull()
            if (lastPeriod == null) return null
            
            val days = ChronoUnit.DAYS.between(lastPeriod, date).toInt() + 1
            return days
        }
    }
}
