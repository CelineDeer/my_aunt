package com.myaunt.app.data

import java.time.LocalDate

/**
 * 白带记录数据模型
 */
data class VaginalDischargeRecord(
    val date: LocalDate,
    val amount: DischargeAmount,
    val color: DischargeColor,
    val texture: DischargeTexture,
    val sensation: DischargeSensation,
    val abdominalDiscomfort: Boolean,
    val afterIntercourse: Boolean,
    val notes: String = "",
)

/**
 * 分泌量
 */
enum class DischargeAmount(val displayName: String) {
    NONE("无"),
    LESS("少量"),
    MEDIUM("中量"),
    MORE("大量");

    companion object {
        fun fromDisplayName(name: String): DischargeAmount? {
            return values().find { it.displayName == name }
        }
    }
}

/**
 * 颜色
 */
enum class DischargeColor(val displayName: String, val colorCode: String) {
    TRANSPARENT("透明", "#E0F7FA"),
    WHITE("乳白", "#FFFFFF"),
    YELLOWISH("发黄", "#FFF9C4"),
    BROWN("褐色", "#D7CCC8");

    companion object {
        fun fromDisplayName(name: String): DischargeColor? {
            return values().find { it.displayName == name }
        }
    }
}

/**
 * 质地
 */
enum class DischargeTexture(val displayName: String) {
    WATERY("水样/清稀"),
    STRETCHY("蛋清状/拉丝"),
    THICK("乳酪状/粘稠"),
    CLUMPY("豆腐渣样");

    companion object {
        fun fromDisplayName(name: String): DischargeTexture? {
            return values().find { it.displayName == name }
        }
    }
}

/**
 * 伴随感觉
 */
enum class DischargeSensation(val displayName: String) {
    FRESH("清爽"),
    ITCHY("瘙痒"),
    ODOR("异味"),
    PAIN("疼痛/灼热");

    companion object {
        fun fromDisplayName(name: String): DischargeSensation? {
            return values().find { it.displayName == name }
        }
    }
}
