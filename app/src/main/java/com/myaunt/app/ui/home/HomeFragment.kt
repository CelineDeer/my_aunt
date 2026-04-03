package com.myaunt.app.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.myaunt.app.R
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.ui.CycleIntervalBand
import com.myaunt.app.ui.applyStatusBarPaddingTop
import com.myaunt.app.ui.UiMotion
import com.myaunt.app.ui.bandForCycleDays
import com.myaunt.app.databinding.FragmentHomeBinding
import com.myaunt.app.MainActivity
import com.myaunt.app.widget.PeriodWidgetUpdater
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PeriodRepository
    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)
    private val todayLineFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)
    private var recordPulseAnimator: AnimatorSet? = null
    private var heartFloatAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        heartFloatAnimator?.cancel()
        heartFloatAnimator = null
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PeriodRepository(requireContext())
        binding.root.applyStatusBarPaddingTop()
        prepHomeEntranceLayout()
        setupClickListeners()
        updateUI()
        scheduleHomeEntrance()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        updateGreetingHeader()
        updateWarmTip()
        updateJourneyCard()

        val days = repository.getDaysSinceLastPeriod()
        val periods = repository.getAllPeriods()

        if (days == null) {
            binding.tvDaysCount.text = "--"
            binding.tvDaysCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_primary))
            binding.tvDaysLabel.text = "还没开始记录，今天先记一笔吧"
            binding.tvLastDate.text = "等待第一次记录 🌸"
            binding.tvCycleInfo.visibility = View.GONE
        } else {
            animateCounter(days)
            val countColor = when (bandForCycleDays(days)) {
                CycleIntervalBand.ABNORMAL -> ContextCompat.getColor(requireContext(), R.color.md_chart_abnormal_text)
                CycleIntervalBand.PURPLE -> ContextCompat.getColor(requireContext(), R.color.md_cycle_purple_text)
                CycleIntervalBand.GREEN -> ContextCompat.getColor(requireContext(), R.color.md_cycle_green_text)
                CycleIntervalBand.YELLOW -> ContextCompat.getColor(requireContext(), R.color.md_cycle_yellow_text)
            }
            binding.tvDaysCount.setTextColor(countColor)
            binding.tvDaysLabel.text = "距离上次姨妈已经"

            val lastDate = periods.first()
            binding.tvLastDate.text = "上次：${lastDate.format(dateFormatter)}"

            val avg = repository.getAverageCycle()
            if (avg != null) {
                val nextDays = avg.toInt() - days
                val wasGone = binding.tvCycleInfo.visibility != View.VISIBLE
                binding.tvCycleInfo.visibility = View.VISIBLE
                binding.tvCycleInfo.text = if (nextDays > 0) {
                    "预计还有 ${nextDays} 天，提前照顾好自己"
                } else {
                    "这两天可能会来，记得早点休息"
                }
                if (wasGone) {
                    binding.tvCycleInfo.alpha = 0f
                    binding.tvCycleInfo.animate()
                        .alpha(1f)
                        .setDuration(320)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            } else {
                binding.tvCycleInfo.animate().cancel()
                binding.tvCycleInfo.visibility = View.GONE
                binding.tvCycleInfo.alpha = 1f
            }
        }

        // Set flower animation based on day count
        updateFlowerAnimation(days)
    }

    private fun homeEntranceViews(): List<View> = listOf(
        binding.cardGreeting,
        binding.cardMainStats,
        binding.cardWarmTip,
        binding.cardJourney,
        binding.barHomeActions,
    )

    private fun prepHomeEntranceLayout() {
        val lift = 40f * resources.displayMetrics.density
        homeEntranceViews().forEach { v ->
            v.animate().cancel()
            v.alpha = 0f
            v.translationY = lift
        }
    }

    private fun scheduleHomeEntrance() {
        binding.root.post {
            if (_binding == null) return@post
            val lift = 40f * resources.displayMetrics.density
            UiMotion.staggerFadeUp(homeEntranceViews(), lift)
            binding.root.postDelayed({
                startHeartFloat()
            }, 520)
        }
    }

    private fun startHeartFloat() {
        heartFloatAnimator?.cancel()
        val iv = _binding?.ivHomeBloom ?: return
        iv.translationY = 0f
        val dy = -10f * resources.displayMetrics.density
        heartFloatAnimator = ObjectAnimator.ofFloat(iv, View.TRANSLATION_Y, 0f, dy).apply {
            duration = 2600
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = PathInterpolator(0.42f, 0f, 0.58f, 1f)
        }
        heartFloatAnimator?.start()
    }

    private fun updateGreetingHeader() {
        binding.tvGreeting.text = greetingForHour(LocalTime.now().hour)
        val today = LocalDate.now()
        binding.tvTodayLine.text = getString(
            R.string.home_today_line,
            today.format(todayLineFormatter),
        )
    }

    private fun greetingForHour(hour: Int): String = when {
        hour < 5 -> getString(R.string.home_greet_late_night)
        hour < 11 -> getString(R.string.home_greet_morning)
        hour < 14 -> getString(R.string.home_greet_noon)
        hour < 18 -> getString(R.string.home_greet_afternoon)
        else -> getString(R.string.home_greet_evening)
    }

    private fun updateWarmTip() {
        val tips = resources.getStringArray(R.array.home_warm_tips)
        if (tips.isEmpty()) return
        val today = LocalDate.now()
        val idx = (today.dayOfYear + today.year * 366) % tips.size
        binding.tvWarmTip.text = tips[idx]
    }

    private fun updateJourneyCard() {
        val periods = repository.getAllPeriods()
        val n = periods.size
        if (n == 0) {
            binding.btnOpenChart.animate().cancel()
            binding.btnOpenChart.visibility = View.GONE
            binding.btnOpenChart.alpha = 1f
            binding.btnOpenChart.scaleX = 1f
            binding.btnOpenChart.scaleY = 1f
            binding.tvJourneySummary.text = getString(R.string.home_journey_empty)
            return
        }
        val lines = mutableListOf<String>()
        lines.add(getString(R.string.home_journey_count, n))
        repository.getAverageCycle()?.let { avg ->
            lines.add(getString(R.string.home_journey_avg, avg))
        }
        lines.add(getString(R.string.home_journey_special_hint))
        binding.tvJourneySummary.text = lines.joinToString("\n")
        val wasGone = binding.btnOpenChart.visibility != View.VISIBLE
        binding.btnOpenChart.visibility = View.VISIBLE
        if (wasGone) {
            binding.btnOpenChart.alpha = 0f
            binding.btnOpenChart.scaleX = 0.94f
            binding.btnOpenChart.scaleY = 0.94f
            binding.btnOpenChart.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(340)
                .setInterpolator(DecelerateInterpolator(1.2f))
                .start()
        }
    }

    private fun animateCounter(targetDays: Long) {
        binding.tvDaysCount.text = targetDays.toString()

        val scaleX = ObjectAnimator.ofFloat(binding.tvDaysCount, "scaleX", 0.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.tvDaysCount, "scaleY", 0.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(binding.tvDaysCount, "alpha", 0f, 1f)
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY, alpha)
        set.duration = 600
        set.interpolator = OvershootInterpolator()
        set.start()
    }

    private fun updateFlowerAnimation(days: Long?) {
        recordPulseAnimator?.cancel()
        val scaleX = ObjectAnimator.ofFloat(binding.btnRecord, "scaleX", 1f, 1.05f, 1f).apply {
            repeatCount = ObjectAnimator.INFINITE
        }
        val scaleY = ObjectAnimator.ofFloat(binding.btnRecord, "scaleY", 1f, 1.05f, 1f).apply {
            repeatCount = ObjectAnimator.INFINITE
        }
        val set = AnimatorSet()
        set.playTogether(scaleX, scaleY)
        set.duration = 1500
        recordPulseAnimator = set
        set.start()
    }

    private fun setupClickListeners() {
        binding.btnRecord.setOnClickListener {
            showRecordConfirmDialog()
        }
        binding.btnBackfill.setOnClickListener {
            showBackfillDatePicker()
        }
        binding.btnOpenChart.setOnClickListener {
            (activity as? MainActivity)?.selectBottomNav(R.id.nav_chart)
        }
    }

    private fun showRecordConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("来姨妈了？")
            .setMessage("确认今天开始了吗？点“是”会记录今天的日期。")
            .setNegativeButton("还没有", null)
            .setPositiveButton("是") { _, _ ->
                handleRecordToday()
            }
            .show()
    }

    private fun handleRecordToday() {
        tryRecordPeriod(LocalDate.now())
    }

    private fun showBackfillDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("补录：来月经的日期")
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            val picked = Instant.ofEpochMilli(utcMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            if (picked.isAfter(LocalDate.now())) return@addOnPositiveButtonClickListener
            // 与日期选择器关闭同一帧链式弹窗时，部分机型上后续对话框无法显示
            binding.root.post {
                if (!isAdded) return@post
                tryRecordPeriod(picked)
            }
        }
        picker.show(parentFragmentManager, "backfill_date")
    }

    private fun tryRecordPeriod(recordDate: LocalDate) {
        if (repository.getAllPeriods().contains(recordDate)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("该日期已有记录")
                .setMessage("${recordDate.format(dateFormatter)} 已经记过一次，无需重复添加。")
                .setPositiveButton("知道了", null)
                .show()
            return
        }

        val abnormalGap = repository.getAbnormalGapForNewRecord(recordDate)
        if (abnormalGap == null) {
            repository.addPeriod(recordDate)
            PeriodWidgetUpdater.updateAll(requireContext())
            updateUI()
            showSuccessAnimation()
            return
        }

        showAbnormalCycleDialog(abnormalGap.days, recordDate, abnormalGap.comparedToFollowingPeriod)
    }

    private fun showAbnormalCycleDialog(
        cycleLength: Long,
        recordDate: LocalDate,
        comparedToFollowingPeriod: Boolean,
    ) {
        val whenText = when {
            recordDate == LocalDate.now() -> "本次"
            comparedToFollowingPeriod ->
                "为 ${recordDate.format(dateFormatter)} 补录时，与之后最近一条记录"
            else ->
                "为 ${recordDate.format(dateFormatter)} 补录时，相对上一次记录"
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("周期提醒")
            .setMessage("$whenText 间隔为 $cycleLength 天，超出常见 20-40 天范围。\n周期可能不太规律，仍要记录吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("仍要记录") { _, _ ->
                showSpecialReasonDialog(cycleLength, recordDate, comparedToFollowingPeriod)
            }
            .show()
    }

    private fun showSpecialReasonDialog(
        cycleLength: Long,
        recordDate: LocalDate,
        comparedToFollowingPeriod: Boolean,
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_special_reason, null, false)
        val tvGapBody = dialogView.findViewById<TextView>(R.id.tv_gap_card_body)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tv_dialog_subtitle)
        val gridChips = dialogView.findViewById<GridLayout>(R.id.grid_reason_chips)
        val noteLayout = dialogView.findViewById<TextInputLayout>(R.id.layout_special_note)
        val noteInput = dialogView.findViewById<TextInputEditText>(R.id.edit_special_note)

        tvSubtitle.text = recordDate.format(dateFormatter)

        val prefix = if (comparedToFollowingPeriod) {
            "本次补录与之后最近一条记录的间隔为 "
        } else {
            "本次记录相对上一次的间隔为 "
        }
        val days = "${cycleLength}天"
        val suffix =
            "，已超出常见的 20–40 天范围。\n请勾选相关标签（可多选）或写一句备注，帮助以后更懂自己的周期。"
        val span = SpannableString(prefix + days + suffix)
        span.setSpan(
            StyleSpan(Typeface.BOLD),
            prefix.length,
            prefix.length + days.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        tvGapBody.text = span

        fun clearNoteError() {
            noteLayout.error = null
        }
        for (i in 0 until gridChips.childCount) {
            val child = gridChips.getChildAt(i)
            if (child is Chip) {
                child.setOnCheckedChangeListener { _, _ -> clearNoteError() }
            }
        }

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (resources.displayMetrics.heightPixels * 0.92f).toInt())
            setGravity(Gravity.BOTTOM)
        }
        @Suppress("DEPRECATION")
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        fun performSave() {
            val tags = collectCheckedReasonChips(gridChips)
            val note = noteInput.text?.toString()?.trim().orEmpty()
            if (tags.isEmpty() && note.isEmpty()) {
                noteLayout.error = "请至少选一个标签，或填写补充说明"
                return
            }
            noteLayout.error = null
            val reason = buildSpecialReasonText(cycleLength, tags, note)
            repository.addPeriodWithSpecialReason(recordDate, reason)
            PeriodWidgetUpdater.updateAll(requireContext())
            dialog.dismiss()
            updateUI()
            showSuccessAnimation()
        }

        dialogView.findViewById<View>(R.id.btn_dialog_close).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.btn_dialog_save_top).setOnClickListener {
            performSave()
        }
        dialogView.findViewById<Button>(R.id.btn_save_record).setOnClickListener {
            performSave()
        }
        dialogView.findViewById<TextView>(R.id.tv_skip_record).setOnClickListener {
            repository.addPeriodWithSpecialReason(recordDate, "未填写")
            PeriodWidgetUpdater.updateAll(requireContext())
            dialog.dismiss()
            updateUI()
            showSuccessAnimation()
        }

        dialog.show()
    }

    private fun collectCheckedReasonChips(grid: GridLayout): List<String> {
        val out = mutableListOf<String>()
        for (i in 0 until grid.childCount) {
            val child = grid.getChildAt(i)
            if (child is Chip && child.isChecked) {
                out.add(child.text.toString())
            }
        }
        return out
    }

    private fun buildSpecialReasonText(cycleLength: Long, tags: List<String>, note: String): String {
        return buildString {
            append("周期${cycleLength}天")
            if (tags.isNotEmpty()) {
                append(" · ")
                append(tags.joinToString("、"))
            }
            if (note.isNotEmpty()) {
                append(" · ")
                append(note)
            }
        }
    }

    private fun showSuccessAnimation() {
        val hint = _binding?.tvSuccessHint ?: return
        hint.visibility = View.VISIBLE
        hint.alpha = 0f
        hint.translationY = 50f

        val fadeIn = ObjectAnimator.ofFloat(hint, "alpha", 0f, 1f)
        val moveUp = ObjectAnimator.ofFloat(hint, "translationY", 50f, 0f)

        val inSet = AnimatorSet()
        inSet.playTogether(fadeIn, moveUp)
        inSet.duration = 400
        inSet.start()

        hint.postDelayed({
            val h = _binding?.tvSuccessHint ?: return@postDelayed
            val fadeOut = ObjectAnimator.ofFloat(h, "alpha", 1f, 0f)
            val outAnim = AnimatorSet()
            outAnim.play(fadeOut)
            outAnim.duration = 600
            outAnim.start()
            h.postDelayed({
                _binding?.tvSuccessHint?.visibility = View.GONE
            }, 600)
        }, 1500)
    }

    override fun onDestroyView() {
        heartFloatAnimator?.cancel()
        heartFloatAnimator = null
        _binding?.ivHomeBloom?.translationY = 0f
        homeEntranceViews().forEach { v ->
            v.animate().cancel()
            v.alpha = 1f
            v.translationY = 0f
        }
        recordPulseAnimator?.cancel()
        recordPulseAnimator = null
        _binding?.tvSuccessHint?.let { hint ->
            hint.animate().cancel()
            hint.handler?.removeCallbacksAndMessages(null)
        }
        super.onDestroyView()
        _binding = null
    }
}
