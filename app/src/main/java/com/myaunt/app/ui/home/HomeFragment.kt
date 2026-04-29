package com.myaunt.app.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.myaunt.app.R
import com.myaunt.app.data.HormoneStatus
import com.myaunt.app.data.HormoneTrend
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.ui.applyStatusBarPaddingTop
import com.myaunt.app.ui.UiMotion
import com.myaunt.app.databinding.FragmentHomeBinding
import com.myaunt.app.MainActivity
import com.myaunt.app.widget.PeriodWidgetUpdater
import java.time.LocalDate
import kotlin.math.round
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PeriodRepository
    private val dateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)
    private val headerDateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINESE)
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

    fun refreshAfterRecord() {
        if (!isAdded || _binding == null) return
        updateUI()
    }

    private fun updateUI() {
        updateMoodHeader()
        updateWarmTip()
        updateJourneyCard()
        updateHormoneDetector()

        val days = repository.getDaysSinceLastPeriod()
        val periods = repository.getAllPeriods()
        val primaryRose = ContextCompat.getColor(requireContext(), R.color.md_primary)

        if (days == null) {
            binding.tvDaysCount.text = "--"
            binding.tvDaysCount.setTextColor(primaryRose)
            binding.tvDaysLabel.text = "距离下次月经还有"
            binding.tvLastDate.text = "天"
            binding.tvCycleInfo.visibility = View.GONE
        } else {
            val avgRounded = repository.getAverageCycle()
                ?.let { round(it).toInt().coerceIn(21, 50) }
                ?: 28
            val daysUntil = (avgRounded - days.toInt()).coerceAtLeast(0)
            animateCounter(daysUntil.toLong())
            binding.tvDaysCount.setTextColor(primaryRose)
            binding.tvDaysLabel.text = "距离下次月经还有"
            binding.tvLastDate.text = "天"

            val cycleLen = avgRounded
            val phase = CycleHormoneMood.inferPhase(days, cycleLen)
            val cycleDay = days.toInt() + 1
            val wasGone = binding.tvCycleInfo.visibility != View.VISIBLE
            binding.tvCycleInfo.visibility = View.VISIBLE
            binding.tvCycleInfo.text = getString(
                R.string.home_cycle_phase_pill,
                phaseDisplayName(phase),
                cycleDay,
            )
            if (wasGone) {
                binding.tvCycleInfo.alpha = 0f
                binding.tvCycleInfo.animate()
                    .alpha(1f)
                    .setDuration(320)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }

        updateFlowerAnimation(days)
    }

    private fun phaseDisplayName(phase: HormoneMoodPhase): String {
        return when (phase) {
            HormoneMoodPhase.MENSTRUAL -> getString(R.string.home_phase_menstrual)
            HormoneMoodPhase.FOLLICULAR -> getString(R.string.home_phase_follicular)
            HormoneMoodPhase.OVULATION -> getString(R.string.home_phase_ovulation)
            HormoneMoodPhase.LUTEAL -> getString(R.string.home_phase_luteal)
            HormoneMoodPhase.PMS_LATE -> getString(R.string.home_phase_pms)
            HormoneMoodPhase.WAITING_NEXT_CYCLE -> getString(R.string.home_phase_waiting)
        }
    }

    private fun homeEntranceViews(): List<View> = listOf(
        binding.cardGreeting,
        binding.cardMainStats,
        binding.hormoneDetector.root,
        binding.cardWarmTip,
        binding.cardJourney,
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

    private fun updateMoodHeader() {
        val today = LocalDate.now()
        binding.tvHomePredictionLabel.text = today.format(headerDateFormatter)
        binding.tvTodayLine.visibility = View.GONE

        val hour = java.time.LocalTime.now().hour
        val salute = when {
            hour < 12 -> getString(R.string.home_salute_morning)
            hour < 18 -> getString(R.string.home_salute_noon)
            else -> getString(R.string.home_salute_evening)
        }
        binding.tvGreeting.text = getString(R.string.home_greeting_line, salute, getString(R.string.home_display_name))
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
        binding.layoutJourneyActions.visibility = View.GONE
        if (n == 0) {
            binding.tvJourneySummary.visibility = View.GONE
            return
        }
        binding.tvJourneySummary.visibility = View.VISIBLE
        binding.tvJourneySummary.text = getString(R.string.home_journey_count, n)
    }

    private fun updateHormoneDetector() {
        val days = repository.getDaysSinceLastPeriod()
        if (days == null) {
            // 没有记录时隐藏激素探测仪
            binding.hormoneDetector.root.visibility = View.GONE
            return
        }

        binding.hormoneDetector.root.visibility = View.VISIBLE
        val cycleDay = days.toInt() + 1
        val hormoneStatus = repository.calculateHormoneStatus(cycleDay)

        // 更新雌激素
        binding.hormoneDetector.tvEstrogenStatus.text = "${hormoneStatus.estrogenTrend.displayName} ${hormoneStatus.estrogenTrend.arrowSymbol}"
        binding.hormoneDetector.progressEstrogen.progress = hormoneStatus.estrogenLevel

        // 更新孕激素
        binding.hormoneDetector.tvProgesteroneStatus.text = "${hormoneStatus.progesteroneTrend.displayName} ${hormoneStatus.progesteroneTrend.arrowSymbol}"
        binding.hormoneDetector.progressProgesterone.progress = hormoneStatus.progesteroneLevel
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
        // 简化动画，不再使用按钮脉冲
    }

    private fun setupClickListeners() {
        binding.btnOpenChart.setOnClickListener {
            (activity as? MainActivity)?.selectBottomNav(R.id.nav_list)
        }
        binding.btnOpenPeriodHistory.setOnClickListener {
            (activity as? MainActivity)?.openPeriodHistory()
        }
        binding.hormoneDetector.btnViewScience.setOnClickListener {
            showScienceDialog()
        }
    }

    private fun showScienceDialog() {
        val scienceText = """
            激素科普：
            
            雌激素：
            - 促进子宫内膜增厚
            - 排卵前达到高峰
            - 影响情绪和皮肤状态
            
            孕激素：
            - 排卵后开始上升
            - 黄体期达到高峰
            - 维持妊娠和体温
            
            提示：此功能仅供参考，不能替代医疗诊断。
        """.trimIndent()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("激素科普")
            .setMessage(scienceText)
            .setPositiveButton("知道了", null)
            .show()
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
        super.onDestroyView()
        _binding = null
    }
}
