package com.myaunt.app.data

/**
 * 激素状态数据模型
 */
data class HormoneStatus(
    val estrogenLevel: Int, // 雌激素水平 0-100
    val estrogenTrend: HormoneTrend, // 雌激素趋势
    val progesteroneLevel: Int, // 孕激素水平 0-100
    val progesteroneTrend: HormoneTrend // 孕激素趋势
)

/**
 * 激素趋势
 */
enum class HormoneTrend(val displayName: String, val arrowSymbol: String) {
    RISING("上升中", "↑"),
    STABLE("平稳状态", ""),
    FALLING("下降中", "↓"),
    LOW("较低", "↓"),
    HIGH("较高", "↑")
}
