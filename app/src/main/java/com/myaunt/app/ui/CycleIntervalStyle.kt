package com.myaunt.app.ui

/** 周期间隔（天）配色：20–24 紫、25–32 绿、33–40 黄；低于 20 或高于 40 为异常（大红）。 */
enum class CycleIntervalBand {
    ABNORMAL,
    PURPLE,
    GREEN,
    YELLOW,
}

fun bandForCycleDays(days: Long): CycleIntervalBand = when {
    days < 20 || days > 40 -> CycleIntervalBand.ABNORMAL
    days in 20..24 -> CycleIntervalBand.PURPLE
    days in 25..32 -> CycleIntervalBand.GREEN
    days in 33..40 -> CycleIntervalBand.YELLOW
    else -> CycleIntervalBand.ABNORMAL
}
