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
}
