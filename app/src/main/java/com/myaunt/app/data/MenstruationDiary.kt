package com.myaunt.app.data

/**
 * 「记录今天」界面保存的非月经开始日日记，或「经期已结束」时的当日记录。
 * 月经开始日仍写入 [PeriodRepository] 的 periods + 可选 special_reasons。
 */
data class MenstruationDiary(
    val flowLabel: String? = null,
    val symptoms: List<String> = emptyList(),
    val mood: String? = null,
    val note: String = "",
    val periodEndedOnly: Boolean = false,
)
