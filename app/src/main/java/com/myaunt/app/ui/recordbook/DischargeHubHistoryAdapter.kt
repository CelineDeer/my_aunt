package com.myaunt.app.ui.recordbook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.myaunt.app.R
import com.myaunt.app.data.DischargeAmount
import com.myaunt.app.data.DischargeColor
import com.myaunt.app.data.DischargeTexture
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.data.VaginalDischargeRecord
import com.myaunt.app.databinding.ItemDischargeHubHistoryBinding
import com.myaunt.app.ui.home.CycleHormoneMood
import com.myaunt.app.ui.home.HormoneMoodPhase
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class DischargeHubHistoryAdapter(
    private val repository: PeriodRepository,
    private val phaseName: (HormoneMoodPhase) -> String,
    private val onItemClick: (VaginalDischargeRecord) -> Unit,
) : RecyclerView.Adapter<DischargeHubHistoryAdapter.VH>() {

    private var records: List<VaginalDischargeRecord> = emptyList()
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINESE)

    fun submitList(list: List<VaginalDischargeRecord>) {
        records = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDischargeHubHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    inner class VH(
        private val binding: ItemDischargeHubHistoryBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: VaginalDischargeRecord) {
            binding.tvStateTitle.text = primaryLabel(record)
            binding.ivStateIcon.setImageResource(iconFor(record))
            val phaseStr = phaseForDate(record.date)?.let { phaseName(it) } ?: "—"
            binding.tvSubtitle.text = "${record.date.format(dateFmt)} · $phaseStr"
            binding.root.setOnClickListener { onItemClick(record) }
        }

        private fun primaryLabel(record: VaginalDischargeRecord): String = when {
            record.amount == DischargeAmount.NONE && record.color == DischargeColor.TRANSPARENT -> "干爽"
            record.texture == DischargeTexture.STRETCHY -> "拉丝"
            record.texture == DischargeTexture.THICK && record.color == DischargeColor.WHITE &&
                record.amount == DischargeAmount.MEDIUM -> "粘稠"
            record.texture == DischargeTexture.THICK && record.color == DischargeColor.WHITE &&
                record.amount == DischargeAmount.LESS -> "乳白"
            record.color == DischargeColor.WHITE && record.texture == DischargeTexture.WATERY -> "乳白"
            else -> "${record.texture.displayName} · ${record.color.displayName}"
        }

        private fun iconFor(record: VaginalDischargeRecord): Int = when (record.texture) {
            DischargeTexture.STRETCHY -> R.drawable.ic_texture_egg
            DischargeTexture.THICK, DischargeTexture.CLUMPY -> R.drawable.ic_texture_thick
            DischargeTexture.WATERY -> R.drawable.ic_water_drop
        }

        private fun phaseForDate(date: java.time.LocalDate): HormoneMoodPhase? {
            val days = daysSinceLastPeriodOn(date) ?: return null
            val avg = repository.getAverageCycle()
                ?.let { kotlin.math.round(it).toInt().coerceIn(21, 50) }
                ?: 28
            return CycleHormoneMood.inferPhase(days, avg)
        }

        private fun daysSinceLastPeriodOn(date: java.time.LocalDate): Long? {
            val periods = repository.getAllPeriods()
            if (periods.isEmpty()) return null
            val last = periods.filter { !it.isAfter(date) }.maxOrNull() ?: return null
            return ChronoUnit.DAYS.between(last, date)
        }
    }
}
