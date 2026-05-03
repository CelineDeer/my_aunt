package com.myaunt.app.ui.record

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.myaunt.app.MainActivity
import com.myaunt.app.R
import com.myaunt.app.data.MenstruationDiary
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.databinding.FragmentRecordTodayBinding
import com.myaunt.app.ui.applyStatusBarPaddingTop
import com.myaunt.app.widget.PeriodWidgetUpdater
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 点击底部加号进入的「记录今天」全屏页，布局参考产品稿。
 */
class RecordTodayFragment : Fragment() {

    private var _binding: FragmentRecordTodayBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_INITIAL_DATE = "initial_date"

        fun newInstance(initialDate: LocalDate): RecordTodayFragment {
            return RecordTodayFragment().apply {
                arguments = bundleOf(ARG_INITIAL_DATE to initialDate.toString())
            }
        }
    }

    private lateinit var repository: PeriodRepository
    private val dateDisplayFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINESE)
    private val dateShortFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)

    private var selectedDate: LocalDate = LocalDate.now()
    private var periodStarted: Boolean = true
    private var selectedFlowLabel: String = ""
    private var selectedMood: String? = null
    private val selectedSymptoms = linkedSetOf<String>()

    private data class FlowOption(val labelRes: Int, val emoji: String, val sizeSp: Float)

    private val flowOptions by lazy {
        listOf(
            FlowOption(R.string.record_today_flow_spotting, "💧", 14f),
            FlowOption(R.string.record_today_flow_light, "💧", 16f),
            FlowOption(R.string.record_today_flow_medium, "💧", 20f),
            FlowOption(R.string.record_today_flow_heavy, "💧", 24f),
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecordTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PeriodRepository(requireContext())
        binding.root.applyStatusBarPaddingTop()
        applyRecordScrollBottomInset()

        arguments?.getString(ARG_INITIAL_DATE)?.let {
            selectedDate = LocalDate.parse(it)
        }

        selectedFlowLabel = getString(R.string.record_today_flow_medium)
        selectedMood = getString(R.string.record_today_mood_calm)
        selectedSymptoms.add(getString(R.string.record_today_symptom_waist))

        setupFlowRow()
        setupSymptomGrid()
        setupMoodRow()
        setupPeriodStateCards()
        updateDateLabel()
        refreshPeriodStateUi()
        refreshFlowUi()
        refreshMoodUi()
        refreshSymptomsUi()

        binding.btnClose.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnPickDate.setOnClickListener { openDatePicker() }

        binding.btnViewAllSymptoms.setOnClickListener {
            val row = binding.rowSymptomsExtra
            row.visibility = if (row.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.btnSave.setOnClickListener { onSaveClicked() }
        setupNoteCounter()
    }

    private fun setupNoteCounter() {
        fun sync() {
            val n = binding.etNote.text?.length ?: 0
            binding.tvNoteCounter.text = "${n.coerceAtMost(200)}/200"
        }
        sync()
        binding.etNote.doAfterTextChanged { sync() }
    }

    /** 主界面底栏叠在全屏 fragment 之上，并处理系统导航栏与键盘。 */
    private fun applyRecordScrollBottomInset() {
        val baseOverlap = resources.getDimensionPixelSize(R.dimen.record_today_scroll_bottom_padding)
        ViewCompat.setOnApplyWindowInsetsListener(binding.recordTodayScroll) { v, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = baseOverlap + maxOf(navBottom, imeBottom))
            insets
        }
        ViewCompat.requestApplyInsets(binding.recordTodayScroll)
    }

    private fun setupFlowRow() {
        val binds = listOf(
            binding.flowSpotting,
            binding.flowLight,
            binding.flowMedium,
            binding.flowHeavy,
        )
        flowOptions.forEachIndexed { i, opt ->
            val b = binds[i]
            b.tvDrop.textSize = opt.sizeSp
            b.tvDrop.text = opt.emoji
            b.tvLabel.setText(opt.labelRes)
            b.card.setOnClickListener {
                selectedFlowLabel = getString(opt.labelRes)
                refreshFlowUi()
            }
        }
    }

    private fun setupSymptomGrid() {
        val specs = listOf(
            Triple(binding.symptomBelly, R.string.record_today_symptom_belly, "🤕"),
            Triple(binding.symptomWaist, R.string.record_today_symptom_waist, "🧍"),
            Triple(binding.symptomHead, R.string.record_today_symptom_head, "🤯"),
            Triple(binding.symptomSkin, R.string.record_today_symptom_skin, "😶"),
            Triple(binding.symptomTired, R.string.record_today_symptom_tired, "⚡"),
            Triple(binding.symptomBloat, R.string.record_today_symptom_bloat, "〰️"),
            Triple(binding.symptomLeg, R.string.record_today_symptom_leg, "🦵"),
            Triple(binding.symptomRash, R.string.record_today_symptom_rash, "🔴"),
            Triple(binding.symptomInsomnia, R.string.record_today_symptom_insomnia, "🌙"),
        )
        for ((inc, strRes, emoji) in specs) {
            val b = inc
            b.tvEmoji.text = emoji
            b.tvLabel.setText(strRes)
            val label = getString(strRes)
            b.card.setOnClickListener {
                if (selectedSymptoms.contains(label)) {
                    selectedSymptoms.remove(label)
                } else {
                    selectedSymptoms.add(label)
                }
                refreshSymptomsUi()
            }
        }
    }

    private fun setupMoodRow() {
        val moods = listOf(
            binding.cardMoodHappy to R.string.record_today_mood_happy,
            binding.cardMoodCalm to R.string.record_today_mood_calm,
            binding.cardMoodSensitive to R.string.record_today_mood_sensitive,
            binding.cardMoodAnxious to R.string.record_today_mood_anxious,
        )
        moods.forEach { (card, res) ->
            card.setOnClickListener {
                selectedMood = getString(res)
                refreshMoodUi()
            }
        }
    }

    private fun setupPeriodStateCards() {
        binding.cardStateStarted.setOnClickListener {
            periodStarted = true
            refreshPeriodStateUi()
        }
        binding.cardStateEnded.setOnClickListener {
            periodStarted = false
            refreshPeriodStateUi()
        }
    }

    private fun refreshPeriodStateUi() {
        val accent = requireContext().getColor(R.color.record_today_accent)
        val stroke = requireContext().getColor(R.color.ui_chart_history_row_stroke)
        val muted = requireContext().getColor(R.color.md_text_secondary)

        binding.cardStateStarted.strokeWidth = if (periodStarted) dp(2) else dp(1)
        binding.cardStateStarted.strokeColor = if (periodStarted) accent else stroke
        binding.cardStateEnded.strokeWidth = if (!periodStarted) dp(2) else dp(1)
        binding.cardStateEnded.strokeColor = if (!periodStarted) accent else stroke

        val titleStarted = (binding.cardStateStarted.getChildAt(0) as ViewGroup).getChildAt(1) as TextView
        val titleEnded = (binding.cardStateEnded.getChildAt(0) as ViewGroup).getChildAt(1) as TextView
        titleStarted.setTextColor(if (periodStarted) accent else muted)
        titleEnded.setTextColor(if (!periodStarted) accent else muted)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun refreshFlowUi() {
        val accent = requireContext().getColor(R.color.record_today_accent)
        val muted = requireContext().getColor(R.color.md_text_secondary)
        val strokeLight = requireContext().getColor(R.color.ui_chart_history_row_stroke)
        val binds = listOf(
            binding.flowSpotting,
            binding.flowLight,
            binding.flowMedium,
            binding.flowHeavy,
        )
        flowOptions.forEachIndexed { i, opt ->
            val b = binds[i]
            val label = getString(opt.labelRes)
            val sel = label == selectedFlowLabel
            b.card.strokeWidth = if (sel) dp(2) else dp(1)
            b.card.strokeColor = if (sel) accent else strokeLight
            b.tvLabel.setTextColor(if (sel) accent else muted)
        }
    }

    private fun refreshSymptomsUi() {
        val accent = requireContext().getColor(R.color.record_today_accent)
        val muted = requireContext().getColor(R.color.md_text_secondary)
        val strokeLight = requireContext().getColor(R.color.ui_chart_history_row_stroke)
        val ids = listOf(
            binding.symptomBelly,
            binding.symptomWaist,
            binding.symptomHead,
            binding.symptomSkin,
            binding.symptomTired,
            binding.symptomBloat,
            binding.symptomLeg,
            binding.symptomRash,
            binding.symptomInsomnia,
        )
        val strRes = listOf(
            R.string.record_today_symptom_belly,
            R.string.record_today_symptom_waist,
            R.string.record_today_symptom_head,
            R.string.record_today_symptom_skin,
            R.string.record_today_symptom_tired,
            R.string.record_today_symptom_bloat,
            R.string.record_today_symptom_leg,
            R.string.record_today_symptom_rash,
            R.string.record_today_symptom_insomnia,
        )
        ids.forEachIndexed { i, inc ->
            val b = inc
            val label = getString(strRes[i])
            val sel = selectedSymptoms.contains(label)
            b.card.strokeWidth = if (sel) dp(2) else dp(1)
            b.card.strokeColor = if (sel) accent else strokeLight
            b.tvLabel.setTextColor(if (sel) accent else muted)
        }
    }

    private fun refreshMoodUi() {
        val accent = requireContext().getColor(R.color.record_today_accent)
        val muted = requireContext().getColor(R.color.md_text_secondary)
        val strokeLight = requireContext().getColor(R.color.ui_chart_history_row_stroke)
        val cards = listOf(
            binding.cardMoodHappy to getString(R.string.record_today_mood_happy),
            binding.cardMoodCalm to getString(R.string.record_today_mood_calm),
            binding.cardMoodSensitive to getString(R.string.record_today_mood_sensitive),
            binding.cardMoodAnxious to getString(R.string.record_today_mood_anxious),
        )
        cards.forEach { (card, label) ->
            val tv = card.getChildAt(0) as TextView
            val sel = label == selectedMood
            card.strokeWidth = if (sel) dp(2) else dp(1)
            card.strokeColor = if (sel) accent else strokeLight
            tv.setTextColor(if (sel) accent else muted)
            tv.typeface = null
            tv.setTypeface(null, if (sel) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun updateDateLabel() {
        binding.tvSelectedDate.text = selectedDate.format(dateDisplayFormatter)
    }

    private fun openDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.record_today_title))
            .setCalendarConstraints(constraints)
            .setSelection(
                selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
            .build()
        picker.addOnPositiveButtonClickListener { utc ->
            selectedDate = Instant.ofEpochMilli(utc).atZone(ZoneId.systemDefault()).toLocalDate()
            if (selectedDate.isAfter(LocalDate.now())) {
                selectedDate = LocalDate.now()
            }
            updateDateLabel()
        }
        picker.show(parentFragmentManager, "record_today_date")
    }

    private fun buildDiarySummary(): String {
        val lines = mutableListOf<String>()
        lines.add(if (periodStarted) "经期已开始" else "经期已结束")
        lines.add("经量：$selectedFlowLabel")
        if (selectedSymptoms.isNotEmpty()) {
            lines.add("症状：" + selectedSymptoms.joinToString("、"))
        }
        selectedMood?.let { lines.add("心情：$it") }
        val note = binding.etNote.text?.toString()?.trim().orEmpty()
        if (note.isNotEmpty()) lines.add("备注：$note")
        return lines.joinToString(" · ")
    }

    private fun onSaveClicked() {
        val diaryText = buildDiarySummary()
        val diary = MenstruationDiary(
            flowLabel = selectedFlowLabel,
            symptoms = selectedSymptoms.toList(),
            mood = selectedMood,
            note = binding.etNote.text?.toString()?.trim().orEmpty(),
            periodEndedOnly = !periodStarted,
        )

        if (!periodStarted) {
            repository.saveMenstruationDiary(selectedDate, diary)
            if (diaryText.isNotBlank()) {
                repository.putSpecialReason(selectedDate, diaryText)
            }
            PeriodWidgetUpdater.updateAll(requireContext())
            Toast.makeText(requireContext(), R.string.record_today_saved_diary, Toast.LENGTH_SHORT).show()
            finishAndRefresh()
            return
        }

        // 经期已开始：写入月经开始日
        if (repository.getAllPeriods().contains(selectedDate)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.period_history_delete_title)
                .setMessage("该日已有月经开始记录，是否覆盖当日备注信息？")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("更新备注") { _, _ ->
                    repository.putSpecialReason(selectedDate, diaryText)
                    PeriodWidgetUpdater.updateAll(requireContext())
                    finishAndRefresh()
                }
                .show()
            return
        }

        val gap = repository.getAbnormalGapForNewRecord(selectedDate)
        if (gap == null) {
            if (diaryText.isNotBlank()) {
                repository.addPeriodWithSpecialReason(selectedDate, diaryText)
            } else {
                repository.addPeriod(selectedDate)
            }
            PeriodWidgetUpdater.updateAll(requireContext())
            repository.saveMenstruationDiary(selectedDate, diary)
            Toast.makeText(requireContext(), R.string.record_today_saved_diary, Toast.LENGTH_SHORT).show()
            finishAndRefresh()
            return
        }

        val whenText = if (selectedDate == LocalDate.now()) "本次" else "为 ${selectedDate.format(dateShortFormatter)} 补录时"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("周期提醒")
            .setMessage("$whenText 间隔为 ${gap.days} 天，超出常见 20–40 天范围。\n仍要记录吗？")
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton("写备注") { _, _ ->
                showAbnormalReasonSheet(
                    gap.days,
                    selectedDate,
                    gap.comparedToFollowingPeriod,
                    diaryText,
                )
            }
            .setPositiveButton("直接记录") { _, _ ->
                repository.addPeriod(selectedDate)
                if (diaryText.isNotBlank()) {
                    repository.putSpecialReason(selectedDate, diaryText)
                }
                repository.saveMenstruationDiary(selectedDate, diary)
                PeriodWidgetUpdater.updateAll(requireContext())
                finishAndRefresh()
            }
            .show()
    }

    private fun showAbnormalReasonSheet(
        cycleLength: Long,
        recordDate: LocalDate,
        comparedToFollowingPeriod: Boolean,
        diaryPrefix: String,
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_special_reason, null, false)
        val gridChips = dialogView.findViewById<GridLayout>(R.id.grid_reason_chips)
        val noteLayout = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layout_special_note)
        val noteInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.edit_special_note)

        dialogView.findViewById<android.widget.TextView>(R.id.tv_dialog_subtitle).text =
            recordDate.format(dateShortFormatter)
        val prefix = if (comparedToFollowingPeriod) {
            "本次补录与之后最近一条记录的间隔为 "
        } else {
            "本次记录相对上一次的间隔为 "
        }
        dialogView.findViewById<android.widget.TextView>(R.id.tv_gap_card_body).text =
            prefix + "${cycleLength}天，已超出常见的 20–40 天范围。\n勾选标签（可多选）或写一句备注。"

        fun collectTags(): List<String> {
            val out = mutableListOf<String>()
            for (i in 0 until gridChips.childCount) {
                val c = gridChips.getChildAt(i)
                if (c is Chip && c.isChecked) out.add(c.text.toString())
            }
            return out
        }

        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.92f).toInt(),
            )
            setGravity(android.view.Gravity.BOTTOM)
        }
        @Suppress("DEPRECATION")
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        fun performSave() {
            val tags = collectTags()
            val note = noteInput.text?.toString()?.trim().orEmpty()
            if (tags.isEmpty() && note.isEmpty()) {
                noteLayout.error = "请至少选一个标签，或填写补充说明"
                return
            }
            noteLayout.error = null
            val reason = buildString {
                append("周期${cycleLength}天")
                if (tags.isNotEmpty()) {
                    append(" · ")
                    append(tags.joinToString("、"))
                }
                if (note.isNotEmpty()) {
                    append(" · ")
                    append(note)
                }
                if (diaryPrefix.isNotBlank()) {
                    append(" · ")
                    append(diaryPrefix)
                }
            }
            repository.addPeriodWithSpecialReason(recordDate, reason)
            repository.saveMenstruationDiary(
                recordDate,
                MenstruationDiary(
                    flowLabel = selectedFlowLabel,
                    symptoms = selectedSymptoms.toList(),
                    mood = selectedMood,
                    note = binding.etNote.text?.toString()?.trim().orEmpty(),
                    periodEndedOnly = false,
                ),
            )
            PeriodWidgetUpdater.updateAll(requireContext())
            dialog.dismiss()
            finishAndRefresh()
        }

        dialogView.findViewById<View>(R.id.btn_dialog_close).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<android.widget.TextView>(R.id.btn_dialog_save_top).setOnClickListener { performSave() }
        dialogView.findViewById<Button>(R.id.btn_save_record).setOnClickListener { performSave() }
        dialogView.findViewById<android.widget.TextView>(R.id.tv_skip_record).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_skip_title)
                .setMessage(R.string.confirm_skip_message)
                .setNegativeButton(R.string.special_dialog_save_record, null)
                .setPositiveButton(R.string.special_dialog_skip) { _, _ ->
                    val skipReason = if (diaryPrefix.isNotBlank()) "未填写 · $diaryPrefix" else "未填写"
                    repository.addPeriodWithSpecialReason(recordDate, skipReason)
                    repository.saveMenstruationDiary(
                        recordDate,
                        MenstruationDiary(
                            flowLabel = selectedFlowLabel,
                            symptoms = selectedSymptoms.toList(),
                            mood = selectedMood,
                            note = binding.etNote.text?.toString()?.trim().orEmpty(),
                            periodEndedOnly = false,
                        ),
                    )
                    PeriodWidgetUpdater.updateAll(requireContext())
                    dialog.dismiss()
                    finishAndRefresh()
                }
                .show()
        }
        dialog.show()
    }

    private fun finishAndRefresh() {
        parentFragmentManager.popBackStack()
        (activity as? MainActivity)?.refreshHomeAfterRecord()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
