package com.myaunt.app.ui.special

/**
 * 解析 [com.myaunt.app.data.SpecialRecord.reason] 展示文案。
 * 保存格式参见 [com.myaunt.app.ui.home.HomeFragment.buildSpecialReasonText]：
 * `周期Nd天 · 标签1、标签2 · 补充说明`
 */
data class ParsedSpecialReason(
    val title: String,
    val days: Int?,
    val durationLabel: String?,
    val tags: List<String>,
    val note: String?,
)

object SpecialReasonDisplayParser {

    private val cycleRegex = Regex("^周期(\\d+)天$")

    fun parse(reason: String): ParsedSpecialReason {
        val raw = reason.trim()
        if (raw.isEmpty() || raw == "未填写") {
            return ParsedSpecialReason(
                title = "未填写说明",
                days = null,
                durationLabel = null,
                tags = emptyList(),
                note = if (raw.isEmpty()) null else raw,
            )
        }

        val parts = raw.split(" · ").map { it.trim() }.filter { it.isNotEmpty() }
        var days: Int? = null
        val nonCycle = mutableListOf<String>()
        for (p in parts) {
            val m = cycleRegex.matchEntire(p)
            if (m != null) {
                days = m.groupValues[1].toIntOrNull()
            } else {
                nonCycle.add(p)
            }
        }

        val tags = mutableListOf<String>()
        var note: String? = null
        when (nonCycle.size) {
            0 -> {}
            1 -> {
                val s = nonCycle[0]
                if (s.contains("、")) {
                    tags.addAll(s.split("、").map { it.trim() }.filter { it.isNotEmpty() })
                } else {
                    note = s
                }
            }
            else -> {
                nonCycle.dropLast(1).forEach { seg ->
                    tags.addAll(seg.split("、").map { it.trim() }.filter { it.isNotEmpty() })
                }
                note = nonCycle.last().takeIf { it.isNotEmpty() }
            }
        }

        val title = when {
            days == null -> "周期说明"
            days >= 90 -> "显著间隔"
            days >= 45 -> "间隔偏长"
            else -> "周期记录"
        }
        val durationLabel = days?.let { "间隔${it}天" }

        return ParsedSpecialReason(
            title = title,
            days = days,
            durationLabel = durationLabel,
            tags = tags,
            note = note?.takeIf { it.isNotEmpty() },
        )
    }
}
