package com.myaunt.app.ui.special

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.myaunt.app.R
import com.myaunt.app.data.SpecialRecord
import com.myaunt.app.databinding.ItemSpecialRecordBinding

class SpecialRecordsAdapter(
    private val onEdit: (SpecialRecord) -> Unit,
    private val onDelete: (SpecialRecord) -> Unit,
) : RecyclerView.Adapter<SpecialRecordsAdapter.VH>() {

    private val items = mutableListOf<SpecialRecord>()

    private val paletteBadge = intArrayOf(
        R.color.md_primary,
        R.color.md_secondary,
        R.color.md_cycle_green_end,
        R.color.md_cycle_purple_end,
        R.color.md_cycle_yellow_end,
        R.color.md_chart_abnormal_end,
    )

    fun submitList(records: List<SpecialRecord>) {
        items.clear()
        items.addAll(records)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSpecialRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(
        private val binding: ItemSpecialRecordBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: SpecialRecord) {
            val ctx = binding.root.context
            val res = ctx.resources
            val parsed = SpecialReasonDisplayParser.parse(record.reason)

            val badgeColor = ContextCompat.getColor(ctx, paletteBadge[paletteIndex(record)])
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(badgeColor)
            }
            binding.dateBadge.background = circle

            val month = record.date.monthValue
            val day = record.date.dayOfMonth
            binding.tvDateBadge.text = ctx.getString(R.string.special_record_badge_date, month, day)

            binding.tvRecordTitle.text = parsed.title

            if (parsed.durationLabel != null) {
                binding.tvDurationBadge.visibility = View.VISIBLE
                binding.tvDurationBadge.text = parsed.durationLabel
            } else {
                binding.tvDurationBadge.visibility = View.GONE
            }

            binding.chipGroupTags.removeAllViews()
            if (parsed.tags.isEmpty()) {
                binding.chipGroupTags.visibility = View.GONE
            } else {
                binding.chipGroupTags.visibility = View.VISIBLE
                val cornerPx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    14f,
                    res.displayMetrics,
                )
                val minH = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    30f,
                    res.displayMetrics,
                ).toInt()
                val padH = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    10f,
                    res.displayMetrics,
                ).toInt()
                for (tag in parsed.tags) {
                    val (bg, fg) = tagColors(ctx, tag)
                    val chip = Chip(ctx).apply {
                        text = tag
                        isCheckable = false
                        isClickable = false
                        isFocusable = false
                        chipStrokeWidth = 0f
                        chipCornerRadius = cornerPx
                        chipMinHeight = minH.toFloat()
                        textStartPadding = padH.toFloat()
                        textEndPadding = padH.toFloat()
                        chipStartPadding = padH.toFloat()
                        chipEndPadding = padH.toFloat()
                        chipBackgroundColor = ColorStateList.valueOf(bg)
                        setTextColor(fg)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }
                    binding.chipGroupTags.addView(chip)
                }
            }

            if (parsed.note != null) {
                binding.tvNotePreview.visibility = View.VISIBLE
                binding.tvNotePreview.text = parsed.note
            } else {
                binding.tvNotePreview.visibility = View.GONE
            }

            binding.btnEdit.setOnClickListener { onEdit(record) }
            binding.btnDelete.setOnClickListener { onDelete(record) }
        }
    }

    private fun paletteIndex(record: SpecialRecord): Int {
        val n = paletteBadge.size
        val v = (record.date.toEpochDay() % n).toInt()
        return (v + n) % n
    }

    private fun tagColors(ctx: android.content.Context, tag: String): Pair<Int, Int> {
        val bg: Int
        val fg: Int
        when {
            tag.contains("压力") -> {
                bg = ContextCompat.getColor(ctx, R.color.md_home_welcome_surface)
                fg = ContextCompat.getColor(ctx, R.color.md_primary)
            }
            tag.contains("熬夜") || tag.contains("作息") -> {
                bg = ContextCompat.getColor(ctx, R.color.md_cycle_purple_start)
                fg = ContextCompat.getColor(ctx, R.color.md_cycle_purple_text)
            }
            tag.contains("饮食") -> {
                bg = ContextCompat.getColor(ctx, R.color.md_cycle_yellow_start)
                fg = ContextCompat.getColor(ctx, R.color.md_cycle_yellow_text)
            }
            tag.contains("生病") || tag.contains("不适") -> {
                bg = ContextCompat.getColor(ctx, R.color.chip_reason_surface)
                fg = ContextCompat.getColor(ctx, R.color.md_chart_abnormal_text)
            }
            tag.contains("情绪") -> {
                bg = ContextCompat.getColor(ctx, R.color.special_note_card_1)
                fg = ContextCompat.getColor(ctx, R.color.md_secondary)
            }
            tag.contains("旅行") || tag.contains("时差") -> {
                bg = ContextCompat.getColor(ctx, R.color.special_note_card_5)
                fg = ContextCompat.getColor(ctx, R.color.md_cycle_green_text)
            }
            tag.contains("药物") -> {
                bg = ContextCompat.getColor(ctx, R.color.special_note_card_4)
                fg = ContextCompat.getColor(ctx, R.color.md_cycle_yellow_text)
            }
            tag.contains("运动") -> {
                bg = ContextCompat.getColor(ctx, R.color.md_cycle_green_start)
                fg = ContextCompat.getColor(ctx, R.color.md_cycle_green_text)
            }
            tag.contains("不清楚") -> {
                bg = ContextCompat.getColor(ctx, R.color.special_unsure_chip_bg)
                fg = ContextCompat.getColor(ctx, R.color.md_on_surface)
            }
            else -> {
                bg = ContextCompat.getColor(ctx, R.color.chip_reason_surface)
                fg = ContextCompat.getColor(ctx, R.color.md_on_surface)
            }
        }
        return bg to fg
    }
}
