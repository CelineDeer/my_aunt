package com.myaunt.app.ui.healthtreasury

import com.myaunt.app.data.Article
import com.myaunt.app.data.ArticleCategory

/**
 * 科普文章列表：摘要用于列表，body 为点击「阅读完整科普」后展示的全文。
 * 文属通俗健康科普，不替代面诊与医嘱。
 */
object HealthTreasuryContent {

    private val articleFollicularDiet = Article(
        id = "1",
        title = "卵泡期怎么吃？",
        subtitle = "饮食建议 · 3分钟阅读",
        summary = "这个阶段由于雌激素上升，代谢变快。建议补充优质蛋白（如豆浆、鱼类）以及抗氧化水果（如蓝莓、草莓）。",
        body = """
            【卵泡期大概是什么时候】
            以一次月经出血「第一天」算作周期起点，卵泡期一般从经期结束到排卵前。多数人整个周期在 21～35 天都常见，你不必和「28 天标准」比个高低，以自己的身体节奏为主。

            【这一阶段身体在做什么（通俗版）】
            雌激素水平逐步回升，身体往往比经期更容易恢复精神、运动意愿也可能更好。没有食欲可以不用硬塞，有胃口时可以趁机把质量提高一点。

            【可以侧重这几类食物】
            1. 优质蛋白：豆浆/豆腐、鱼、蛋、鸡猪牛瘦肉。帮助组织修复与稳定饱腹感。
            2. 全谷物与杂豆：燕麦、糙米、小米、红小豆等，搭配蔬菜，对血糖波动更友好。
            3. 彩虹蔬果：深绿色叶菜、彩椒、莓果、柑橘等，维 C 和多种植化素，有助于吃够膳食纤维。
            4. 健康脂肪一小把：核桃、杏仁等坚果，或橄榄油烹调，但热量不低，小份量即可。

            【小提醒】
            • 没生病不必「补过头」，均衡比名贵食材更重要。
            • 有慢性病、控糖控盐医嘱或正在服药时，以医生或营养师的个体方案为准。
            本文仅作生活中参考，不替代诊疗。
        """.trimIndent(),
        category = ArticleCategory.HEALTH_DIET,
        iconRes = android.R.drawable.ic_menu_info_details,
        iconBackgroundColor = "#E3F2FD",
        cardBackgroundColor = "#FFF5F7",
    )

    private val articleHormoneCycle = Article(
        id = "2",
        title = "了解你的激素周期",
        subtitle = "深度科普 · 5分钟阅读",
        summary = "你知道吗？你的身体在28天里会经历四种不同的激素主导阶段：经期、卵泡期、排卵期和黄体期...",
        body = """
            【为什么要懂「周期感」】
            月经是子宫内膜周期变化的一部分，背后和大脑—卵巢轴、雌激素、孕激素等激素的起伏相关。知道大致阶段，能帮你更温柔地理解：为什么有时想动、有时想静，而不是苛责自己「怎么又不对劲」。

            【一个常被提起的时间轴（以约 28 天为例，仅作示意）】
            1. 经期（行经期）：出血的几天。内膜脱落，有人乏力、腹坠都常见。
            2. 卵泡期：从经期到排卵前。卵泡在发育，雌激素多呈上升趋势。
            3. 排卵期：通常在下次月经前约 14 天（因人而异）。可伴随蛋清状分泌物、单侧下腹酸胀等，也有人毫无感觉。
            4. 黄体期：排卵后到下次月经前。黄体分泌孕激素，若未怀孕，激素回落，月经来潮。

            【和「心情」可以怎样相处】
            激素波动和情绪、睡眠、压力都有关，但不等于你的一切感受都能用激素解释。可以观察自己的规律、休息和求助同样重要。若长期情绪低落、经前或经期严重影响生活，应就医排查。

            【和本应用里示意功能的说明】
            你见到的「阶段示意、激素趣味科普」是简化教学模型，不能用来诊断疾病或决定用药。有疑虑时请咨询正规医疗机构。
        """.trimIndent(),
        category = ArticleCategory.PERIOD_CARE,
        iconRes = android.R.drawable.ic_menu_info_details,
        iconBackgroundColor = "#F3E5F5",
        cardBackgroundColor = "#F0F4FF",
    )

    private val articlePmsLifestyle = Article(
        id = "3",
        title = "缓解经前综合症(PMS)",
        subtitle = "生活方式 · 4分钟阅读",
        summary = "经前综合症可以通过调整饮食、适量运动和充足睡眠来缓解。试试这些方法...",
        body = """
            【可能的表现】
            经前数天至两周内出现情绪低落、烦躁、乳胀、头痛、想吃东西、睡不稳等，且明显影响工作学习或关系时，值得重视。很多人症状轻微，通过生活方式就能舒服很多；若很重，应就医。

            【可以一步步试的生活策略】
            1. 稳定血糖：少一顿暴饮大量甜食，改为规律三餐，碳水搭配蛋白和蔬菜。
            2. 动一动，温柔即可：散步、瑜伽、游泳等中等强度、可持续的运动，对情绪和睡眠常有帮助。不必在不舒服时强撑剧烈运动。
            3. 睡够与光线：相对固定就寝、起床时间；晚上减少长时间刷高刺激内容。
            4. 补镁/维 B 族？ 有人从食物或补充剂中获益，但补充剂与药物相互作用因人而异，有疾病或正在服药者请先问医生或药师。

            【何时需要看医生】
            情绪崩溃、自伤想法、经前无法起床上班上课、或疼痛剧烈等，都不要再独自硬扛。PMS/经前恶情绪障碍（PMDD）有医学干预手段，可挂号妇科或精神心理相关门诊评估。

            本文仅供科普，不替代面诊与处方。
        """.trimIndent(),
        category = ArticleCategory.LIFESTYLE,
        iconRes = android.R.drawable.ic_menu_info_details,
        iconBackgroundColor = "#FFF8E1",
        cardBackgroundColor = "#FFF8E7",
    )

    private val articlePeriodCare = Article(
        id = "4",
        title = "经期护理指南",
        subtitle = "经期护理 · 6分钟阅读",
        summary = "经期是女性身体最脆弱的时期，需要特别注意保暖、卫生和休息。了解正确的护理方法...",
        body = """
            【卫生与用品】
            • 卫生巾、棉条、月经杯都各有使用方法与更换频率。原则是：按说明书选择合适型号、及时更换、保持手与用品清洁。棉条与月经杯不建议超时留置。
            • 有瘙痒、异常气味、或分泌物颜色明显异常，应停用过香产品，尽快就医，勿自行长期用药。

            【保暖与休息】
            下腹、腰背保暖常能缓解不适。热水袋敷肚子以「舒服、不烫」为准。少熬夜，痛经明显时可把日程安排得松一些。

            【可以洗澡吗】
            可以淋浴；盆浴、公共泳池是否适合要看个人情况与习惯，有炎症或刚术后请遵医嘱。

            【痛到什么程度要就医】
            吃止痛药仍完全无法缓解、经量突然明显变多或有大血块、发热、晕厥等，都应及时就诊，排除需处理的疾病。痛经不是「必须忍一辈子」的。

            【和亲密行为】
            经期是否可以性生活因人而异，以双方舒适、卫生与防感染为主；若有不适或医嘱限制，以医护意见为准。

            本指南为普适性护理建议，不替代个人诊疗。
        """.trimIndent(),
        category = ArticleCategory.PERIOD_CARE,
        iconRes = android.R.drawable.ic_menu_info_details,
        iconBackgroundColor = "#E8F5E9",
        cardBackgroundColor = "#F5F5F5",
    )

    private val articleIronRichFoods = Article(
        id = "5",
        title = "补血食谱推荐",
        subtitle = "健康饮食 · 5分钟阅读",
        summary = "经期后需要补充铁质，红枣、桂圆、猪肝都是很好的选择。推荐几款简单易做的补血食谱...",
        body = """
            【先弄清「补什么」】
            月经过多或已确诊缺铁性贫血时，应遵医嘱补铁。食物里的铁分血红素铁（如动物肝脏、红肉、血类）和非血红素铁（如菠菜、黑木耳、红豆）。维 C 能帮助下吸收非血红素铁，茶与浓咖啡同餐会抑制吸收，可错开 1～2 小时。

            【三道简单家常思路（示例）】
            1. 菠菜拌豆腐 + 柑橘：豆腐提供蛋白，菠菜焯水去草酸，再配小半颗橙或几瓣橘子。
            2. 番茄炖牛腩：番茄维 C 与红肉中的铁搭配；牛腩肥瘦可自选，少油烹调。
            3. 猪肝汤（偶尔适量）：高胆固醇或血脂异常者要控制频率与量，可换瘦肉、鸡肝，由医生/营养师定份量。

            【关于红枣、桂圆、红糖水】
            红枣、桂圆主要带来碳水与一些微量元素，对「提气色」有心理与热量上的满足，但指望单独「补血治贫血」不现实。红糖与普通糖热量相近，痛经时热饮的安慰感是真的，但不必过量。

            【需要就医的信号】
            脸唇苍白、动一动就慌、经量明显增多或持续乏力，要查血常规与原因，别只靠食疗拖延。

            文为饮食科普，不替代检查与治疗。
        """.trimIndent(),
        category = ArticleCategory.HEALTH_DIET,
        iconRes = android.R.drawable.ic_menu_info_details,
        iconBackgroundColor = "#FFEBEE",
        cardBackgroundColor = "#FFF5F7",
    )

    val articles: List<Article> = listOf(
        articleFollicularDiet,
        articleHormoneCycle,
        articlePmsLifestyle,
        articlePeriodCare,
        articleIronRichFoods,
    )
}
