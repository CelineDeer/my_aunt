package com.myaunt.app.ui.chart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.myaunt.app.ui.UiMotion
import com.myaunt.app.ui.applyStatusBarPaddingTop
import com.myaunt.app.MainActivity
import com.myaunt.app.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.myaunt.app.data.PeriodRepository
import com.myaunt.app.databinding.FragmentChartBinding
import com.myaunt.app.databinding.ItemChartPageBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class ChartFragment : Fragment() {
    private data class ResolvedRange(
        val startMonth: YearMonth,
        val endMonth: YearMonth,
        val buttonText: String,
        val hintText: String,
    )

    private data class MonthlyCycle(
        val month: YearMonth,
        val periodDate: LocalDate,
        val intervalDays: Long,
    )

    private data class ChartWindow(
        val startMonth: YearMonth,
        val endMonth: YearMonth,
        val cycles: List<MonthlyCycle>,
        val hormonePoints: List<BarChartView.HormonePoint>,
    )

    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: PeriodRepository
    private val fullDateFormat = DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)
    private val chartHistoryDayFormat = DateTimeFormatter.ofPattern("MM.dd", Locale.CHINESE)
    private val monthFormat = DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINESE)
    private var chartEntrancePlayedForView = false
    private var chartEntrancePostPending = false
    private var monthlyCycles: List<MonthlyCycle> = emptyList()
    private var windows: List<ChartWindow> = emptyList()
    private var selectedRangeStartMonth: YearMonth? = null
    private var selectedRangeEndMonth: YearMonth? = null
    private var rangePresetMonths: Int? = 6
    private var resolvedRange: ResolvedRange? = null
    private lateinit var pagerAdapter: ChartPagerAdapter
    private var pagerCallback: ViewPager2.OnPageChangeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        chartEntrancePlayedForView = false
        chartEntrancePostPending = false
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PeriodRepository(requireContext())
        binding.root.applyStatusBarPaddingTop()
        pagerAdapter = ChartPagerAdapter()
        binding.chartPager.adapter = pagerAdapter
        pagerCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                renderPage(position)
            }
        }
        binding.chartPager.registerOnPageChangeCallback(pagerCallback!!)
        setupRangeQuickFilters()
        binding.btnChartBack.setOnClickListener {
            (activity as? MainActivity)?.selectBottomNav(R.id.nav_home)
        }
        refreshAllData()
        scheduleChartEntrance()

        // 启动激素曲线动画
        binding.hormoneCurveView.startAnimation()
    }

    override fun onResume() {
        super.onResume()
        refreshAllData()
        binding.hormoneCurveView.startAnimation()
    }

    private fun refreshAllData() {
        monthlyCycles = buildMonthlyCycles(repository.getAllPeriods())
        val dataMinMonth = monthlyCycles.minByOrNull { it.month }?.month
        val dataMaxMonth = monthlyCycles.maxByOrNull { it.month }?.month
        val defaultEnd = dataMaxMonth ?: YearMonth.now()
        val defaultMonths = (rangePresetMonths ?: 6).toLong()
        val defaultStart = maxOf(
            dataMinMonth ?: defaultEnd.minusMonths(defaultMonths - 1),
            defaultEnd.minusMonths(defaultMonths - 1),
        )
        if (selectedRangeStartMonth == null && selectedRangeEndMonth == null) {
            selectedRangeStartMonth = defaultStart
            selectedRangeEndMonth = defaultEnd
        } else {
            val nowMonth = YearMonth.now()
            selectedRangeStartMonth = selectedRangeStartMonth?.coerceAtMost(nowMonth)
            selectedRangeEndMonth = selectedRangeEndMonth?.coerceAtMost(nowMonth)
        }
        rebuildWindowsAndRender()
        populateChartHistory()
    }

    private fun populateChartHistory() {
        val container = binding.containerChartHistory
        container.removeAllViews()
        val cycles = monthlyCycles.sortedByDescending { it.periodDate }.take(5)
        if (cycles.isEmpty()) {
            binding.sectionChartHistory.visibility = View.GONE
            return
        }
        binding.sectionChartHistory.visibility = View.VISIBLE
        val inflater = layoutInflater
        for (mc in cycles) {
            val row = inflater.inflate(R.layout.item_chart_history_row, container, false)
            row.findViewById<TextView>(R.id.tvHistoryTitle).text =
                getString(R.string.chart_history_month_title, mc.month.monthValue)
            row.findViewById<TextView>(R.id.tvHistorySubtitle).text = getString(
                R.string.chart_history_subtitle,
                mc.periodDate.format(chartHistoryDayFormat),
                mc.intervalDays,
            )
            row.findViewById<TextView>(R.id.tvHistoryInterval).text =
                getString(R.string.chart_history_interval_end, mc.intervalDays)
            container.addView(row)
        }
    }

    private fun buildMonthlyCycles(periodsDesc: List<LocalDate>): List<MonthlyCycle> {
        if (periodsDesc.size < 2) return emptyList()
        val byMonth = linkedMapOf<YearMonth, MonthlyCycle>()
        for (i in 0 until periodsDesc.size - 1) {
            val periodDate = periodsDesc[i]
            val previous = periodsDesc[i + 1]
            val intervalDays = ChronoUnit.DAYS.between(previous, periodDate)
            if (intervalDays <= 0) continue
            val month = YearMonth.from(periodDate)
            if (!byMonth.containsKey(month)) {
                byMonth[month] = MonthlyCycle(
                    month = month,
                    periodDate = periodDate,
                    intervalDays = intervalDays,
                )
            }
        }
        // 时间最早的一条间隔缺少更早记录作为参照，不参与图表展示
        return byMonth.values.sortedBy { it.month }.drop(1)
    }

    private fun rebuildWindowsAndRender() {
        val resolved = resolveRangeSelection()
        resolvedRange = resolved
        selectedRangeStartMonth = resolved.startMonth
        selectedRangeEndMonth = resolved.endMonth
        syncRangeControls()

        windows = buildWindows(resolved.startMonth, resolved.endMonth)
        pagerAdapter.submit(windows)
        if (windows.isEmpty()) {
            renderNoWindowState(resolved)
            return
        }
        val lastIndex = windows.lastIndex
        binding.chartPager.setCurrentItem(lastIndex, false)
        renderPage(lastIndex)
    }

    private fun resolveRangeSelection(): ResolvedRange {
        val maxSpanMonths = 12L
        val rawStart = selectedRangeStartMonth
        val rawEnd = selectedRangeEndMonth
        val nowMonth = YearMonth.now()

        if (rawStart != null && rawEnd != null) {
            val start = minOf(rawStart, rawEnd)
            val end = maxOf(rawStart, rawEnd)
            val span = ChronoUnit.MONTHS.between(start, end) + 1
            return if (span <= maxSpanMonths) {
                ResolvedRange(
                    startMonth = start,
                    endMonth = end,
                    buttonText = "日期范围：${start.atDay(1).format(monthFormat)} - ${end.atDay(1).format(monthFormat)}",
                    hintText = "已按所选月份范围展示",
                )
            } else {
                val clippedEnd = start.plusMonths(maxSpanMonths - 1).coerceAtMost(nowMonth)
                ResolvedRange(
                    startMonth = start,
                    endMonth = clippedEnd,
                    buttonText = "日期范围：${start.atDay(1).format(monthFormat)} - ${clippedEnd.atDay(1).format(monthFormat)}",
                    hintText = "所选范围超过12个月，已按起始月份截取前12个月",
                )
            }
        }

        if (rawStart != null) {
            val start = rawStart
            val end = minOf(start.plusMonths(maxSpanMonths - 1), nowMonth)
            return ResolvedRange(
                startMonth = start,
                endMonth = end,
                buttonText = "开始月份：${start.atDay(1).format(monthFormat)}",
                hintText = "仅选了开始月份，已自动向后展示最多12个月",
            )
        }

        if (rawEnd != null) {
            val end = rawEnd
            val start = end.minusMonths(maxSpanMonths - 1)
            return ResolvedRange(
                startMonth = start,
                endMonth = end,
                buttonText = "结束月份：${end.atDay(1).format(monthFormat)}",
                hintText = "仅选了结束月份，已自动向前展示最多12个月",
            )
        }

        val fallbackEnd = nowMonth
        val fallbackStart = fallbackEnd.minusMonths(maxSpanMonths - 1)
        return ResolvedRange(
            startMonth = fallbackStart,
            endMonth = fallbackEnd,
            buttonText = "日期范围：${fallbackStart.atDay(1).format(monthFormat)} - ${fallbackEnd.atDay(1).format(monthFormat)}",
            hintText = "默认展示最近12个月",
        )
    }

    private fun buildWindows(rangeStart: YearMonth, rangeEnd: YearMonth): List<ChartWindow> {
        val out = mutableListOf<ChartWindow>()
        var cursor = rangeStart
        val periods = repository.getAllPeriods()
        val basePeriodDate = periods.firstOrNull() ?: LocalDate.now()
        
        while (!cursor.isAfter(rangeEnd)) {
            val endMonth = cursor
            val startMonth = endMonth.minusMonths(11)
            val cycles = monthlyCycles.filter {
                it.month >= startMonth && it.month <= endMonth && it.month >= rangeStart && it.month <= rangeEnd
            }
            
            // 计算激素数据
            val hormonePoints = if (cycles.isNotEmpty()) {
                val startDate = cycles.first().periodDate
                val endDate = cycles.last().periodDate
                repository.calculateHormoneCurveData(startDate, endDate, basePeriodDate)
            } else {
                emptyList()
            }
            
            out.add(ChartWindow(startMonth = startMonth, endMonth = endMonth, cycles = cycles, hormonePoints = hormonePoints))
            cursor = cursor.plusMonths(1)
        }
        return out
    }

    private fun renderNoWindowState(range: ResolvedRange) {
        binding.btnPickMonth.text = range.buttonText
        binding.tvWindowRange.text = "${range.hintText}，当前窗口无数据"
        binding.chartPager.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = "所选时间范围内暂无周期数据，请换个日期范围看看"
        binding.cardStats.visibility = View.GONE
    }

    private fun renderPage(position: Int) {
        if (position !in windows.indices) return
        val window = windows[position]
        val intervals = window.cycles.map { it.intervalDays }
        val avg = intervals.takeIf { it.isNotEmpty() }?.average()
        val abnormalCount = intervals.count { it < 20 || it > 40 }
        val chartPoints = window.cycles.map {
            BarChartView.ChartPoint(
                periodDate = it.periodDate,
                intervalDays = it.intervalDays,
            )
        }

        val range = resolvedRange ?: return
        binding.btnPickMonth.text = range.buttonText
        binding.tvWindowRange.text =
            "${range.hintText}；窗口：${window.startMonth.atDay(1).format(monthFormat)} - ${window.endMonth.atDay(1).format(monthFormat)}（最多12个月）"

        binding.chartPager.visibility = View.VISIBLE
        if (chartPoints.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "所选时间范围内暂无周期数据，请换个日期看看"
            binding.cardStats.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.cardStats.visibility = View.VISIBLE

            // 应用内未单独记录经期天数时，与 doc 一致展示常见均值示意
            binding.tvRecordCount.text = getString(R.string.chart_avg_menses_days)
            avg?.let {
                binding.tvAvgCycle.text = "%.0f天".format(it)
            }

            val min = intervals.minOrNull()
            val max = intervals.maxOrNull()
            binding.tvMinCycle.text = "${min}天"
            binding.tvMaxCycle.text = "${max}天"
            binding.tvAbnormal.text = "${abnormalCount}次"
        }
    }

    private fun showMonthRangePickerDialog(markAsCustom: Boolean) {
        val currentStart = selectedRangeStartMonth ?: YearMonth.now().minusMonths(11)
        val currentEnd = selectedRangeEndMonth ?: YearMonth.now()
        val baseMin = minOf(monthlyCycles.minByOrNull { it.month }?.month ?: currentStart, currentStart)
        val baseMax = maxOf(monthlyCycles.maxByOrNull { it.month }?.month ?: currentEnd, currentEnd)
        // 给月份滚轮留出上下缓冲，避免默认选中项贴边，观感更居中。
        val minMonth = baseMin.minusMonths(6)
        val maxMonth = minOf(YearMonth.now(), baseMax.plusMonths(6))

        val dialogView = layoutInflater.inflate(R.layout.dialog_month_range_picker, null, false)
        val startMonthPicker = dialogView.findViewById<NumberPicker>(R.id.pickerStartMonth)
        val endMonthPicker = dialogView.findViewById<NumberPicker>(R.id.pickerEndMonth)

        val allMonths = mutableListOf<YearMonth>()
        var cursor = minMonth
        while (!cursor.isAfter(maxMonth)) {
            allMonths.add(cursor)
            cursor = cursor.plusMonths(1)
        }
        val monthLabels = allMonths.map { it.atDay(1).format(monthFormat) }.toTypedArray()
        val displayedValues = arrayOf("未选择") + monthLabels

        fun setupMonthPicker(picker: NumberPicker, selected: YearMonth?) {
            picker.displayedValues = null
            picker.minValue = 0
            picker.maxValue = displayedValues.lastIndex
            picker.displayedValues = displayedValues
            picker.value = selected?.let { allMonths.indexOf(it).takeIf { index -> index >= 0 }?.plus(1) } ?: 0
            picker.wrapSelectorWheel = false
            picker.setOnLongPressUpdateInterval(120)
        }

        setupMonthPicker(startMonthPicker, currentStart)
        setupMonthPicker(endMonthPicker, currentEnd)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择月份范围")
            .setView(dialogView)
            .setNegativeButton("取消") { _, _ ->
                syncRangeControls()
            }
            .setPositiveButton("确定") { _, _ ->
                val pickedStart = allMonths.getOrNull(startMonthPicker.value - 1)
                val pickedEnd = allMonths.getOrNull(endMonthPicker.value - 1)
                if (pickedStart == null && pickedEnd == null) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("范围不完整")
                        .setMessage("请至少选择开始月份或结束月份其中一个。")
                        .setPositiveButton("知道了", null)
                        .show()
                    syncRangeControls()
                    return@setPositiveButton
                }
                if (pickedStart != null && pickedEnd != null && pickedStart.isAfter(pickedEnd)) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("范围不正确")
                        .setMessage("起始月份不能晚于结束月份。")
                        .setPositiveButton("知道了", null)
                        .show()
                    syncRangeControls()
                    return@setPositiveButton
                }
                if (markAsCustom) {
                    rangePresetMonths = null
                }
                selectedRangeStartMonth = pickedStart
                selectedRangeEndMonth = pickedEnd
                rebuildWindowsAndRender()
            }
            .create()
        dialog.setOnCancelListener { syncRangeControls() }
        dialog.show()
    }

    private fun setupRangeQuickFilters() {
        binding.chipRange3m.setOnClickListener { applyPresetRange(3) }
        binding.chipRange6m.setOnClickListener { applyPresetRange(6) }
        binding.chipRange12m.setOnClickListener { applyPresetRange(12) }
        binding.chipRangeCustom.setOnClickListener { showMonthRangePickerDialog(markAsCustom = true) }
        binding.btnPickMonth.setOnClickListener { showMonthRangePickerDialog(markAsCustom = true) }
    }

    private fun applyPresetRange(months: Int) {
        rangePresetMonths = months
        val endMonth = monthlyCycles.maxByOrNull { it.month }?.month ?: YearMonth.now()
        val startMonth = endMonth.minusMonths((months - 1).toLong())
        selectedRangeStartMonth = startMonth
        selectedRangeEndMonth = endMonth
        rebuildWindowsAndRender()
    }

    private fun syncRangeControls() {
        when (rangePresetMonths) {
            3 -> binding.chipRange3m.isChecked = true
            6 -> binding.chipRange6m.isChecked = true
            12 -> binding.chipRange12m.isChecked = true
            else -> binding.chipRangeCustom.isChecked = true
        }
        binding.btnPickMonth.visibility = if (rangePresetMonths == null) View.VISIBLE else View.GONE
    }

    private fun chartEntranceTargets(): List<View> {
        val out = mutableListOf<View>(
            binding.btnChartBack,
            binding.tvChartHeadline,
            binding.chipGroupRange,
            binding.tvChartLegend,
            binding.btnPickMonth,
            binding.tvWindowRange,
            binding.cardChartArea,
            binding.cardHormoneCurve,
            binding.sectionChartHistory,
        )
        if (binding.tvEmpty.visibility == View.VISIBLE) {
            out.add(binding.tvEmpty)
        }
        if (binding.cardStats.visibility == View.VISIBLE) {
            out.add(binding.cardStats)
        }
        return out
    }

    private fun scheduleChartEntrance() {
        if (chartEntrancePlayedForView || chartEntrancePostPending) return
        chartEntrancePostPending = true
        val lift = 32f * resources.displayMetrics.density
        chartEntranceTargets().forEach { v ->
            v.animate().cancel()
            v.alpha = 0f
            v.translationY = lift
        }
        binding.root.post {
            chartEntrancePostPending = false
            if (_binding == null || chartEntrancePlayedForView) return@post
            chartEntrancePlayedForView = true
            UiMotion.staggerFadeUp(chartEntranceTargets(), lift, itemDelayMs = 42)
        }
    }

    override fun onDestroyView() {
        binding.btnChartBack.animate().cancel()
        binding.tvChartHeadline.animate().cancel()
        binding.chipGroupRange.animate().cancel()
        binding.tvChartLegend.animate().cancel()
        binding.btnPickMonth.animate().cancel()
        binding.tvWindowRange.animate().cancel()
        binding.cardChartArea.animate().cancel()
        binding.cardHormoneCurve.animate().cancel()
        binding.sectionChartHistory.animate().cancel()
        binding.tvEmpty.animate().cancel()
        binding.cardStats.animate().cancel()
        binding.btnChartBack.alpha = 1f
        binding.btnChartBack.translationY = 0f
        binding.tvChartHeadline.alpha = 1f
        binding.tvChartHeadline.translationY = 0f
        binding.chipGroupRange.alpha = 1f
        binding.chipGroupRange.translationY = 0f
        binding.tvChartLegend.alpha = 1f
        binding.tvChartLegend.translationY = 0f
        binding.btnPickMonth.alpha = 1f
        binding.btnPickMonth.translationY = 0f
        binding.tvWindowRange.alpha = 1f
        binding.tvWindowRange.translationY = 0f
        binding.cardChartArea.alpha = 1f
        binding.cardChartArea.translationY = 0f
        binding.cardHormoneCurve.alpha = 1f
        binding.cardHormoneCurve.translationY = 0f
        binding.sectionChartHistory.alpha = 1f
        binding.sectionChartHistory.translationY = 0f
        binding.tvEmpty.alpha = 1f
        binding.tvEmpty.translationY = 0f
        binding.cardStats.alpha = 1f
        binding.cardStats.translationY = 0f
        pagerCallback?.let { binding.chartPager.unregisterOnPageChangeCallback(it) }
        pagerCallback = null
        super.onDestroyView()
        _binding = null
    }

    private inner class ChartPagerAdapter : RecyclerView.Adapter<ChartPagerAdapter.VH>() {
        private val items = mutableListOf<ChartWindow>()

        fun submit(data: List<ChartWindow>) {
            items.clear()
            items.addAll(data)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemChartPageBinding.inflate(inflater, parent, false)
            return VH(binding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val window = items[position]
            val chartPoints = window.cycles.map {
                BarChartView.ChartPoint(
                    periodDate = it.periodDate,
                    intervalDays = it.intervalDays,
                )
            }
            
            // 传递激素数据到图表
            if (window.hormonePoints.isNotEmpty() && window.hormonePoints.size == chartPoints.size) {
                holder.binding.pageChartView.setData(chartPoints, window.hormonePoints)
            } else {
                holder.binding.pageChartView.setData(chartPoints)
            }
            
            holder.binding.pageChartView.onBarLongPressListener = { periodDate, intervalDays ->
                val note = repository.getSpecialReason(periodDate)
                val title = "${periodDate.format(fullDateFormat)} · 间隔 ${intervalDays} 天"
                val message = note?.takeIf { it.isNotBlank() }
                    ?: "这次记录没有填写备注。\n（周期异常时保存的说明会显示在这里）"
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("知道了", null)
                    .show()
            }
        }

        inner class VH(val binding: ItemChartPageBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
