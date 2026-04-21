package com.myaunt.app.ui.healthtreasury

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.myaunt.app.R
import com.myaunt.app.data.Article
import com.myaunt.app.data.ArticleCategory
import com.myaunt.app.databinding.FragmentHealthTreasuryBinding

class HealthTreasuryFragment : Fragment() {

    private var _binding: FragmentHealthTreasuryBinding? = null
    private val binding get() = _binding!!
    private lateinit var articleAdapter: ArticleAdapter

    // 所有文章数据
    private val allArticles = listOf(
        Article(
            id = "1",
            title = "卵泡期怎么吃？",
            subtitle = "饮食建议 · 3分钟阅读",
            summary = "这个阶段由于雌激素上升，代谢变快。建议补充优质蛋白（如豆浆、鱼类）以及抗氧化水果（如蓝莓、草莓）。",
            category = ArticleCategory.HEALTH_DIET,
            iconRes = android.R.drawable.ic_menu_info_details,
            iconBackgroundColor = "#E3F2FD",
            cardBackgroundColor = "#FFF5F7",
            isExpanded = false
        ),
        Article(
            id = "2",
            title = "了解你的激素周期",
            subtitle = "深度科普 · 5分钟阅读",
            summary = "你知道吗？你的身体在28天里会经历四种不同的激素主导阶段：经期、卵泡期、排卵期和黄体期...",
            category = ArticleCategory.PERIOD_CARE,
            iconRes = android.R.drawable.ic_menu_info_details,
            iconBackgroundColor = "#F3E5F5",
            cardBackgroundColor = "#F0F4FF",
            isExpanded = false
        ),
        Article(
            id = "3",
            title = "缓解经前综合症(PMS)",
            subtitle = "生活方式 · 4分钟阅读",
            summary = "经前综合症可以通过调整饮食、适量运动和充足睡眠来缓解。试试这些方法...",
            category = ArticleCategory.LIFESTYLE,
            iconRes = android.R.drawable.ic_menu_info_details,
            iconBackgroundColor = "#FFF8E1",
            cardBackgroundColor = "#FFF8E7",
            isExpanded = false
        ),
        Article(
            id = "4",
            title = "经期护理指南",
            subtitle = "经期护理 · 6分钟阅读",
            summary = "经期是女性身体最脆弱的时期，需要特别注意保暖、卫生和休息。了解正确的护理方法...",
            category = ArticleCategory.PERIOD_CARE,
            iconRes = android.R.drawable.ic_menu_info_details,
            iconBackgroundColor = "#E8F5E9",
            cardBackgroundColor = "#F5F5F5",
            isExpanded = false
        ),
        Article(
            id = "5",
            title = "补血食谱推荐",
            subtitle = "健康饮食 · 5分钟阅读",
            summary = "经期后需要补充铁质，红枣、桂圆、猪肝都是很好的选择。推荐几款简单易做的补血食谱...",
            category = ArticleCategory.HEALTH_DIET,
            iconRes = android.R.drawable.ic_menu_info_details,
            iconBackgroundColor = "#FFEBEE",
            cardBackgroundColor = "#FFF5F7",
            isExpanded = false
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHealthTreasuryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCategoryChips()
        filterArticles(ArticleCategory.ALL)
    }

    private fun setupRecyclerView() {
        articleAdapter = ArticleAdapter()
        binding.recyclerArticles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = articleAdapter
        }
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chipPeriodCare -> ArticleCategory.PERIOD_CARE
                R.id.chipHealthDiet -> ArticleCategory.HEALTH_DIET
                R.id.chipLifestyle -> ArticleCategory.LIFESTYLE
                else -> ArticleCategory.ALL
            }
            filterArticles(category)
        }

        // 设置分类标签选中效果
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            updateChipStyle(binding.chipAll, isChecked)
        }
        binding.chipPeriodCare.setOnCheckedChangeListener { _, isChecked ->
            updateChipStyle(binding.chipPeriodCare, isChecked)
        }
        binding.chipHealthDiet.setOnCheckedChangeListener { _, isChecked ->
            updateChipStyle(binding.chipHealthDiet, isChecked)
        }
        binding.chipLifestyle.setOnCheckedChangeListener { _, isChecked ->
            updateChipStyle(binding.chipLifestyle, isChecked)
        }
    }

    private fun updateChipStyle(chip: com.google.android.material.chip.Chip, isChecked: Boolean) {
        if (isChecked) {
            chip.setChipBackgroundColorResource(R.color.md_primary)
            chip.setTextColor(Color.WHITE)
        } else {
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
            chip.setTextColor(Color.parseColor("#E91E63"))
        }
    }

    private fun filterArticles(category: ArticleCategory) {
        val filteredArticles = if (category == ArticleCategory.ALL) {
            allArticles
        } else {
            allArticles.filter { it.category == category }
        }
        articleAdapter.submitList(filteredArticles)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
