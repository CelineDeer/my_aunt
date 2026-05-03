package com.myaunt.app.ui.recordbook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.myaunt.app.MainActivity
import com.myaunt.app.R
import com.myaunt.app.data.DischargeAmount
import com.myaunt.app.data.DischargeColor
import com.myaunt.app.data.DischargeSensation
import com.myaunt.app.data.DischargeTexture
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.data.VaginalDischargeRecord
import com.myaunt.app.databinding.FragmentRecordBookBinding
import com.myaunt.app.ui.applyStatusBarPaddingTop
import com.myaunt.app.ui.home.CycleHormoneMood
import com.myaunt.app.ui.home.HormoneMoodPhase
import com.myaunt.app.ui.vaginaldischarge.VaginalDischargeHistoryFragment
import com.myaunt.app.widget.PeriodWidgetUpdater
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.round

/**
 * 白带记录中心：周期情境、健康分析、快捷状态、历史列表（单页）。
 */
class RecordBookFragment : Fragment() {

    companion object {
        const val RESULT_DISCHARGE_DATE = "record_book_discharge_date"
        const val EXTRA_DATE = "date"
        private const val HISTORY_PREVIEW = 5
    }

    private enum class QuickOption {
        DRY,
        THICK,
        MILKY,
        STRETCHY,
        ;

        fun toPreset(): Triple<DischargeAmount, DischargeColor, DischargeTexture> = when (this) {
            DRY -> Triple(DischargeAmount.NONE, DischargeColor.TRANSPARENT, DischargeTexture.WATERY)
            THICK -> Triple(DischargeAmount.MEDIUM, DischargeColor.WHITE, DischargeTexture.THICK)
            MILKY -> Triple(DischargeAmount.LESS, DischargeColor.WHITE, DischargeTexture.THICK)
            STRETCHY -> Triple(DischargeAmount.LESS, DischargeColor.WHITE, DischargeTexture.STRETCHY)
        }
    }

    private var _binding: FragmentRecordBookBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: PeriodRepository
    private lateinit var hubHistoryAdapter: DischargeHubHistoryAdapter

    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedQuick: QuickOption? = null
    private var pendingDateFromHistory: LocalDate? = null

    private val dateHeaderFmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINESE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().supportFragmentManager.setFragmentResultListener(
            RESULT_DISCHARGE_DATE,
            this,
        ) { _, bundle ->
            val str = bundle.getString(EXTRA_DATE) ?: return@setFragmentResultListener
            runCatching { LocalDate.parse(str) }.getOrNull()?.let { pendingDateFromHistory = it }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecordBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyStatusBarPaddingTop()
        repository = PeriodRepository(requireContext())

        hubHistoryAdapter = DischargeHubHistoryAdapter(
            repository = repository,
            phaseName = { phaseDisplayName(it) },
            onItemClick = { openDischargeAtDate(it.date) },
        )
        binding.recyclerHubHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHubHistory.adapter = hubHistoryAdapter

        binding.tvRecordDate.setOnClickListener { showRecordDatePicker() }
        binding.btnCycleCalendar.setOnClickListener { showRecordDatePicker() }
        binding.tvLinkChangeOnHome.setOnClickListener {
            (activity as? MainActivity)?.selectBottomNav(R.id.nav_home)
        }

        binding.cardQuickDry.setOnClickListener { selectQuick(QuickOption.DRY) }
        binding.cardQuickThick.setOnClickListener { selectQuick(QuickOption.THICK) }
        binding.cardQuickMilky.setOnClickListener { selectQuick(QuickOption.MILKY) }
        binding.cardQuickStretchy.setOnClickListener { selectQuick(QuickOption.STRETCHY) }

        binding.btnSaveRecord.setOnClickListener { saveRecord() }
        binding.tvViewMoreHistory.setOnClickListener { openFullHistory() }
        binding.fabQuickAdd.setOnClickListener {
            selectedDate = LocalDate.now()
            updateDateHeader()
            loadFromRepository()
            refreshCycleBanner()
            refreshAnalysis()
            binding.scrollRecordHub.post {
                binding.scrollRecordHub.smoothScrollTo(0, binding.sectionRecord.top)
            }
        }

        updateDateHeader()
        loadFromRepository()
        refreshCycleBanner()
        refreshAnalysis()
        refreshHubHistory()
    }

    override fun onResume() {
        super.onResume()
        pendingDateFromHistory?.let { date ->
            pendingDateFromHistory = null
            openDischargeAtDate(date)
        }
        refreshCycleBanner()
        refreshAnalysis()
        refreshHubHistory()
    }

    /** 从历史点某条：回到本页并选中该日。 */
    fun openDischargeAtDate(date: LocalDate) {
        if (_binding == null || !isAdded) return
        selectedDate = date
        updateDateHeader()
        loadFromRepository()
        refreshCycleBanner()
        refreshAnalysis()
        binding.scrollRecordHub.post {
            binding.scrollRecordHub.smoothScrollTo(0, binding.sectionRecord.top)
        }
    }

    private fun selectQuick(option: QuickOption) {
        selectedQuick = option
        applyQuickCardVisuals()
        refreshAnalysis()
    }

    private fun applyQuickCardVisuals() {
        val pairs = listOf(
            QuickOption.DRY to Triple(binding.cardQuickDry, binding.ivCheckDry, binding.flIconDry),
            QuickOption.THICK to Triple(binding.cardQuickThick, binding.ivCheckThick, binding.flIconThick),
            QuickOption.MILKY to Triple(binding.cardQuickMilky, binding.ivCheckMilky, binding.flIconMilky),
            QuickOption.STRETCHY to Triple(binding.cardQuickStretchy, binding.ivCheckStretchy, binding.flIconStretchy),
        )
        pairs.forEach { (opt, triple) ->
            val (card, check, iconWrap) = triple
            val selected = opt == selectedQuick
            card.strokeWidth = if (selected) (3 * resources.displayMetrics.density).toInt() else (1 * resources.displayMetrics.density).toInt()
            card.strokeColor = resources.getColor(
                if (selected) R.color.md_primary else R.color.chip_reason_stroke_unchecked,
                null,
            )
            check.visibility = if (selected) View.VISIBLE else View.GONE
            iconWrap.setBackgroundResource(
                if (selected) R.drawable.bg_hub_history_icon_selected else R.drawable.bg_hub_history_icon,
            )
        }
    }

    private fun loadFromRepository() {
        val ex = repository.getVaginalDischargeRecord(selectedDate)
        selectedQuick = if (ex != null) matchQuickOption(ex) else null
        applyQuickCardVisuals()
    }

    private fun matchQuickOption(record: VaginalDischargeRecord): QuickOption? {
        val clean = record.sensation == DischargeSensation.FRESH &&
            !record.abdominalDiscomfort &&
            !record.afterIntercourse &&
            record.notes.isBlank()
        if (!clean) return null
        return QuickOption.entries.find { opt ->
            val (a, c, t) = opt.toPreset()
            record.amount == a && record.color == c && record.texture == t
        }
    }

    private fun updateDateHeader() {
        binding.tvRecordDate.text = selectedDate.format(dateHeaderFmt)
    }

    private fun showRecordDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()
        val selectionMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择日期")
            .setCalendarConstraints(constraints)
            .setSelection(selectionMillis)
            .build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            val picked = Instant.ofEpochMilli(utcMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            if (picked.isAfter(LocalDate.now())) return@addOnPositiveButtonClickListener
            selectedDate = picked
            updateDateHeader()
            loadFromRepository()
            refreshCycleBanner()
            refreshAnalysis()
        }
        picker.show(parentFragmentManager, "hub_discharge_date")
    }

    private fun daysSinceLastPeriodOn(date: LocalDate): Long? {
        val periods = repository.getAllPeriods()
        if (periods.isEmpty()) return null
        val last = periods.filter { !it.isAfter(date) }.maxOrNull() ?: return null
        return ChronoUnit.DAYS.between(last, date)
    }

    private fun refreshCycleBanner() {
        val days = daysSinceLastPeriodOn(selectedDate)
        if (days == null) {
            binding.tvCyclePhaseLabel.text = getString(R.string.record_hub_phase_line, "—")
            binding.tvCycleDaysBig.text = getString(R.string.record_hub_cycle_no_period)
        } else {
            val avg = repository.getAverageCycle()
                ?.let { round(it).toInt().coerceIn(21, 50) }
                ?: 28
            val phase = CycleHormoneMood.inferPhase(days, avg)
            binding.tvCyclePhaseLabel.text = getString(
                R.string.record_hub_phase_line,
                phaseDisplayName(phase),
            )
            val untilNext = (avg - days.toInt()).coerceAtLeast(0)
            binding.tvCycleDaysBig.text = getString(R.string.record_hub_days_until_next, untilNext)
        }
    }

    private fun phaseDisplayName(phase: HormoneMoodPhase): String = when (phase) {
        HormoneMoodPhase.MENSTRUAL -> getString(R.string.home_phase_menstrual)
        HormoneMoodPhase.FOLLICULAR -> getString(R.string.home_phase_follicular)
        HormoneMoodPhase.OVULATION -> getString(R.string.home_phase_ovulation)
        HormoneMoodPhase.LUTEAL -> getString(R.string.home_phase_luteal)
        HormoneMoodPhase.PMS_LATE -> getString(R.string.home_phase_pms)
        HormoneMoodPhase.WAITING_NEXT_CYCLE -> getString(R.string.home_phase_waiting)
    }

    private fun refreshAnalysis() {
        val days = daysSinceLastPeriodOn(selectedDate)
        val avg = repository.getAverageCycle()
            ?.let { round(it).toInt().coerceIn(21, 50) }
            ?: 28
        val phase = if (days != null) CycleHormoneMood.inferPhase(days, avg) else null

        val (tipRes, sugRes) = when {
            phase == null -> R.string.record_hub_tip_no_cycle to R.string.record_hub_sug_no_cycle
            phase == HormoneMoodPhase.MENSTRUAL ->
                R.string.record_hub_tip_menstrual to R.string.record_hub_sug_menstrual
            phase == HormoneMoodPhase.OVULATION -> when (selectedQuick) {
                QuickOption.STRETCHY ->
                    R.string.record_hub_tip_ovulation_stretchy to R.string.record_hub_sug_ovulation
                else ->
                    R.string.record_hub_tip_ovulation_other to R.string.record_hub_sug_ovulation
            }
            phase == HormoneMoodPhase.LUTEAL -> when (selectedQuick) {
                QuickOption.THICK, QuickOption.MILKY ->
                    R.string.record_hub_tip_luteal_thick to R.string.record_hub_sug_luteal
                else ->
                    R.string.record_hub_tip_luteal_other to R.string.record_hub_sug_luteal
            }
            else -> R.string.record_hub_tip_default to R.string.record_hub_sug_default
        }
        binding.tvAnalysisTip.text = getString(tipRes)
        binding.tvAnalysisSuggestion.text = getString(sugRes)
    }

    private fun saveRecord() {
        val quick = selectedQuick ?: run {
            Toast.makeText(requireContext(), R.string.record_hub_pick_state_first, Toast.LENGTH_SHORT).show()
            return
        }
        val (amount, color, texture) = quick.toPreset()
        val record = VaginalDischargeRecord(
            date = selectedDate,
            amount = amount,
            color = color,
            texture = texture,
            sensation = DischargeSensation.FRESH,
            abdominalDiscomfort = false,
            afterIntercourse = false,
            notes = "",
        )
        repository.saveVaginalDischargeRecord(record)
        PeriodWidgetUpdater.updateAll(requireContext())
        Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
        refreshHubHistory()
        binding.scrollRecordHub.post {
            binding.scrollRecordHub.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun refreshHubHistory() {
        val all = repository.getAllVaginalDischargeRecords()
        if (all.isEmpty()) {
            binding.tvHistoryEmpty.visibility = View.VISIBLE
            binding.recyclerHubHistory.visibility = View.GONE
            binding.tvViewMoreHistory.visibility = View.GONE
            hubHistoryAdapter.submitList(emptyList())
            return
        }
        binding.tvHistoryEmpty.visibility = View.GONE
        binding.recyclerHubHistory.visibility = View.VISIBLE
        val preview = all.take(HISTORY_PREVIEW)
        hubHistoryAdapter.submitList(preview)
        binding.tvViewMoreHistory.visibility = if (all.size > HISTORY_PREVIEW) View.VISIBLE else View.GONE
    }

    private fun openFullHistory() {
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
            .replace(R.id.fragment_container, VaginalDischargeHistoryFragment.newInstance(false))
            .addToBackStack("discharge_full_history")
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
