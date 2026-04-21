package com.myaunt.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 新增记录时若与相邻记录间隔异常（非 20–40 天），携带间隔天数及参照方向 */
data class AbnormalRecordGap(val days: Long, val comparedToFollowingPeriod: Boolean)

class PeriodRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("period_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PERIODS = "periods"
        private const val KEY_SPECIAL_REASONS = "special_reasons"
        private const val KEY_VAGINAL_DISCHARGE_RECORDS = "vaginal_discharge_records"
    }

    fun getAllPeriods(): List<LocalDate> {
        val json = prefs.getString(KEY_PERIODS, "[]") ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        val dateStrings: List<String> = gson.fromJson(json, type)
        return dateStrings.map { LocalDate.parse(it) }.sortedDescending()
    }

    fun addPeriod(date: LocalDate) {
        val periods = getAllPeriods().toMutableList()
        if (!periods.contains(date)) {
            periods.add(date)
            savePeriods(periods)
        }
    }

    fun addPeriodWithSpecialReason(date: LocalDate, reason: String) {
        addPeriod(date)
        val reasons = getSpecialReasons().toMutableMap()
        reasons[date.toString()] = reason
        saveSpecialReasons(reasons)
    }

    fun getLastCycleLength(referenceDate: LocalDate = LocalDate.now()): Long? {
        val lastPeriod = getAllPeriods().firstOrNull() ?: return null
        return ChronoUnit.DAYS.between(lastPeriod, referenceDate)
    }

    /**
     * 准备新增 [recordDate] 时，若与**相邻**已有记录（之前最近一条、或之后最近一条）的间隔不在 20–40 天，返回异常信息。
     * 补录日期早于当前所有记录时，仅存在「下一条」，此前会误判为无需校验；此处一并检测。
     * 无任何相邻记录（首条记录）或两侧间隔均在常见范围内时返回 null。
     */
    fun getAbnormalGapForNewRecord(recordDate: LocalDate): AbnormalRecordGap? {
        val periods = getAllPeriods()
        val previous = periods.filter { it.isBefore(recordDate) }.maxOrNull()
        val following = periods.filter { it.isAfter(recordDate) }.minOrNull()

        fun abnormal(d: Long) = d < 20 || d > 40

        val fromPrevious = previous?.let { ChronoUnit.DAYS.between(it, recordDate) }?.takeIf { it > 0 }
        val toFollowing = following?.let { ChronoUnit.DAYS.between(recordDate, it) }?.takeIf { it > 0 }

        if (fromPrevious != null && abnormal(fromPrevious)) {
            return AbnormalRecordGap(fromPrevious, comparedToFollowingPeriod = false)
        }
        if (toFollowing != null && abnormal(toFollowing)) {
            return AbnormalRecordGap(toFollowing, comparedToFollowingPeriod = true)
        }
        return null
    }

    private fun savePeriods(periods: List<LocalDate>) {
        val dateStrings = periods.map { it.toString() }
        val json = gson.toJson(dateStrings)
        prefs.edit().putString(KEY_PERIODS, json).apply()
    }

    fun getDaysSinceLastPeriod(): Long? {
        val periods = getAllPeriods()
        if (periods.isEmpty()) return null
        val lastPeriod = periods.first()
        return ChronoUnit.DAYS.between(lastPeriod, LocalDate.now())
    }

    /**
     * 相邻两次月经开始日之间的间隔（天），按时间从新到旧遍历。
     */
    fun getCycleIntervals(): List<Long> {
        val periods = getAllPeriods().sortedDescending()
        if (periods.size < 2) return emptyList()
        val intervals = mutableListOf<Long>()
        for (i in 0 until periods.size - 1) {
            val days = ChronoUnit.DAYS.between(periods[i + 1], periods[i])
            if (days > 0) intervals.add(days)
        }
        return intervals
    }

    /**
     * 历史相邻月经间隔（天）的算术平均；至少 **2 条** 记录才有 1 个间隔，此时平均值即该间隔。
     *
     * UI 推测：`剩余天数 ≈ round(平均值) − 距上次月经天数`；
     * `预计来潮日 ≈ 上次月经日 + round(平均值)`。
     */
    fun getAverageCycle(): Double? {
        val intervals = getCycleIntervals()
        if (intervals.isEmpty()) return null
        return intervals.average()
    }

    private fun getSpecialReasons(): Map<String, String> {
        val json = prefs.getString(KEY_SPECIAL_REASONS, "{}") ?: "{}"
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson<Map<String, String>>(json, type) ?: emptyMap()
    }

    private fun saveSpecialReasons(reasons: Map<String, String>) {
        val json = gson.toJson(reasons)
        prefs.edit().putString(KEY_SPECIAL_REASONS, json).apply()
    }

    fun getSpecialRecords(): List<SpecialRecord> {
        return getSpecialReasons().mapNotNull { (key, reason) ->
            runCatching { LocalDate.parse(key) }.getOrNull()?.let { SpecialRecord(it, reason) }
        }.sortedByDescending { it.date }
    }

    fun getSpecialReason(date: LocalDate): String? {
        return getSpecialReasons()[date.toString()]
    }

    fun updateSpecialReason(date: LocalDate, reason: String) {
        val trimmed = reason.trim()
        if (trimmed.isEmpty()) return
        val reasons = getSpecialReasons().toMutableMap()
        if (reasons.containsKey(date.toString())) {
            reasons[date.toString()] = trimmed
            saveSpecialReasons(reasons)
        }
    }

    fun removeSpecialReason(date: LocalDate) {
        val reasons = getSpecialReasons().toMutableMap()
        reasons.remove(date.toString())
        saveSpecialReasons(reasons)
    }

    // ============ 白带记录相关方法 ============

    fun saveVaginalDischargeRecord(record: VaginalDischargeRecord) {
        val records = getVaginalDischargeRecordsMap().toMutableMap()
        records[record.date.toString()] = record
        saveVaginalDischargeRecordsMap(records)
    }

    fun getVaginalDischargeRecord(date: LocalDate): VaginalDischargeRecord? {
        return getVaginalDischargeRecordsMap()[date.toString()]
    }

    fun getAllVaginalDischargeRecords(): List<VaginalDischargeRecord> {
        return getVaginalDischargeRecordsMap().values.sortedByDescending { it.date }
    }

    fun deleteVaginalDischargeRecord(date: LocalDate) {
        val records = getVaginalDischargeRecordsMap().toMutableMap()
        records.remove(date.toString())
        saveVaginalDischargeRecordsMap(records)
    }

    private fun getVaginalDischargeRecordsMap(): Map<String, VaginalDischargeRecord> {
        val json = prefs.getString(KEY_VAGINAL_DISCHARGE_RECORDS, "{}") ?: "{}"
        val type = object : TypeToken<Map<String, VaginalDischargeRecord>>() {}.type
        return gson.fromJson<Map<String, VaginalDischargeRecord>>(json, type) ?: emptyMap()
    }

    private fun saveVaginalDischargeRecordsMap(records: Map<String, VaginalDischargeRecord>) {
        val json = gson.toJson(records)
        prefs.edit().putString(KEY_VAGINAL_DISCHARGE_RECORDS, json).apply()
    }

    /**
     * 检测连续指定天数内是否有相同的异常感觉
     */
    fun hasConsecutiveSensation(
        sensation: DischargeSensation,
        consecutiveDays: Int,
        referenceDate: LocalDate = LocalDate.now()
    ): Boolean {
        val records = getAllVaginalDischargeRecords()
        var count = 0
        var currentDate = referenceDate

        for (i in 0 until consecutiveDays) {
            val record = records.find { it.date == currentDate }
            if (record?.sensation == sensation) {
                count++
            } else {
                return false
            }
            currentDate = currentDate.minusDays(1)
        }
        return count == consecutiveDays
    }

    /**
     * 获取指定日期范围内的白带记录
     */
    fun getVaginalDischargeRecordsInRange(startDate: LocalDate, endDate: LocalDate): List<VaginalDischargeRecord> {
        return getAllVaginalDischargeRecords().filter { 
            it.date in startDate..endDate 
        }.sortedBy { it.date }
    }

    /**
     * 生成就诊摘要（过去30天的记录）
     */
    fun generateMedicalSummary(days: Int = 30): String {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong())
        val records = getVaginalDischargeRecordsInRange(startDate, endDate)

        if (records.isEmpty()) {
            return "过去${days}天无白带记录"
        }

        val summary = StringBuilder()
        summary.append("就诊摘要（过去${days}天）\n")
        summary.append("记录天数：${records.size}天\n\n")

        // 统计各感觉的出现次数
        val sensationCount = records.groupingBy { it.sensation }.eachCount()
        summary.append("伴随感觉统计：\n")
        sensationCount.forEach { (sensation, count) ->
            summary.append("- ${sensation.displayName}：${count}次\n")
        }
        summary.append("\n")

        // 统计颜色分布
        val colorCount = records.groupingBy { it.color }.eachCount()
        summary.append("颜色分布：\n")
        colorCount.forEach { (color, count) ->
            summary.append("- ${color.displayName}：${count}次\n")
        }
        summary.append("\n")

        // 统计质地分布
        val textureCount = records.groupingBy { it.texture }.eachCount()
        summary.append("质地分布：\n")
        textureCount.forEach { (texture, count) ->
            summary.append("- ${texture.displayName}：${count}次\n")
        }
        summary.append("\n")

        // 异常记录
        val abnormalRecords = records.filter { it.sensation != DischargeSensation.FRESH }
        if (abnormalRecords.isNotEmpty()) {
            summary.append("异常记录日期：\n")
            abnormalRecords.forEach { record ->
                summary.append("- ${record.date}：${record.sensation.displayName}")
                if (record.notes.isNotEmpty()) {
                    summary.append("（${record.notes}）")
                }
                summary.append("\n")
            }
        }

        return summary.toString()
    }

    // ============ 激素状态计算方法 ============

    /**
     * 根据周期天数计算激素状态
     * @param cycleDay 周期天数（从上次月经开始算起）
     * @return 激素状态
     */
    fun calculateHormoneStatus(cycleDay: Int): HormoneStatus {
        // 简化的激素模型，基于28天周期
        // 雌激素：卵泡期逐渐上升，排卵期达到高峰，黄体期下降
        // 孕激素：排卵前很低，排卵后上升，黄体期达到高峰，月经前下降
        
        val estrogenLevel = when {
            cycleDay <= 5 -> 20f + (cycleDay * 5f) // 月经期：20-45
            cycleDay <= 14 -> 45f + ((cycleDay - 5) * 6f) // 卵泡期到排卵：45-99
            cycleDay <= 21 -> 99f - ((cycleDay - 14) * 5f) // 黄体期早期下降：99-64
            else -> 64f - ((cycleDay - 21) * 4f) // 黄体期晚期：64-28
        }.coerceIn(0f, 100f)

        val progesteroneLevel = when {
            cycleDay <= 13 -> 10f + (cycleDay * 2f) // 排卵前很低：10-36
            cycleDay <= 21 -> 36f + ((cycleDay - 13) * 8f) // 黄体期上升：36-100
            else -> 100f - ((cycleDay - 21) * 10f) // 月经前下降：100-20
        }.coerceIn(0f, 100f)

        val estrogenTrend = when {
            cycleDay <= 14 -> HormoneTrend.RISING
            cycleDay <= 21 -> HormoneTrend.FALLING
            else -> HormoneTrend.FALLING
        }

        val progesteroneTrend = when {
            cycleDay <= 14 -> HormoneTrend.RISING
            cycleDay <= 21 -> HormoneTrend.RISING
            else -> HormoneTrend.FALLING
        }

        return HormoneStatus(
            estrogenLevel = estrogenLevel.toInt(),
            progesteroneLevel = progesteroneLevel.toInt(),
            estrogenTrend = estrogenTrend,
            progesteroneTrend = progesteroneTrend
        )
    }

    /**
     * 计算指定日期范围内的激素变化曲线数据
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param basePeriodDate 基准月经日期（用于计算周期天数）
     * @return 激素点列表
     */
    fun calculateHormoneCurveData(
        startDate: LocalDate,
        endDate: LocalDate,
        basePeriodDate: LocalDate
    ): List<com.myaunt.app.ui.chart.BarChartView.HormonePoint> {
        val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toInt()
        val hormonePoints = mutableListOf<com.myaunt.app.ui.chart.BarChartView.HormonePoint>()
        
        for (i in 0..daysBetween) {
            val currentDate = startDate.plusDays(i.toLong())
            val cycleDay = ChronoUnit.DAYS.between(basePeriodDate, currentDate).toInt() + 1
            val hormoneStatus = calculateHormoneStatus(cycleDay)
            
            hormonePoints.add(
                com.myaunt.app.ui.chart.BarChartView.HormonePoint(
                    estrogenLevel = hormoneStatus.estrogenLevel.toFloat(),
                    progesteroneLevel = hormoneStatus.progesteroneLevel.toFloat()
                )
            )
        }
        
        return hormonePoints
    }
}
