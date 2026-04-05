package com.myaunt.app.ui.home

import kotlin.math.max

/**
 * 按「距上次月经第一天」与周期长度，把当前日子映射到常见的激素相关阶段（教科书式示意）。
 * 不测量真实激素水平，个体差异大，不能代替医疗判断。
 */
enum class HormoneMoodPhase {
    MENSTRUAL,
    FOLLICULAR,
    OVULATION,
    LUTEAL,
    PMS_LATE,
    /** 已超过按周期长度推算的「下一周期起点」，可能推迟或尚未记录新周期 */
    WAITING_NEXT_CYCLE,
}

object CycleHormoneMood {

    fun inferPhase(daysSinceLastPeriodStart: Long, cycleLengthDays: Int): HormoneMoodPhase {
        val L = cycleLengthDays.coerceIn(21, 50)
        val c = (daysSinceLastPeriodStart + 1).toInt().coerceAtLeast(1)
        if (c > L) return HormoneMoodPhase.WAITING_NEXT_CYCLE

        val mEnd = (L * 20 / 100).coerceIn(3, 7)
        val ovStart = max(mEnd + 1, L * 38 / 100)
        val ovEnd = max(ovStart, L * 53 / 100)
        val pmsLen = max(5, L * 22 / 100)
        val lateStart = L - pmsLen + 1

        return when {
            c <= mEnd -> HormoneMoodPhase.MENSTRUAL
            c < ovStart -> HormoneMoodPhase.FOLLICULAR
            c <= ovEnd -> HormoneMoodPhase.OVULATION
            c < lateStart -> HormoneMoodPhase.LUTEAL
            else -> HormoneMoodPhase.PMS_LATE
        }
    }
}
