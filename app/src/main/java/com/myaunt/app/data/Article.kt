package com.myaunt.app.data

enum class ArticleCategory {
    ALL,          // 全部
    PERIOD_CARE,  // 经期护理
    HEALTH_DIET,  // 健康饮食
    LIFESTYLE     // 生活方式
}

data class Article(
    val id: String,
    val title: String,
    val subtitle: String,      // 副标题，如"饮食建议 · 3分钟阅读"
    val summary: String,       // 摘要内容
    val category: ArticleCategory,
    val iconRes: Int,          // 图标资源
    val iconBackgroundColor: String,  // 图标背景色
    val cardBackgroundColor: String,  // 卡片背景色
    val isExpanded: Boolean = false   // 是否展开显示完整内容
)
