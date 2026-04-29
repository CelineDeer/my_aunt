package com.myaunt.app.ui.healthtreasury

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.myaunt.app.R
import com.myaunt.app.data.Article
import com.myaunt.app.databinding.ItemArticleBinding

class ArticleAdapter : RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    private var articles = listOf<Article>()
    private val expandedPositions = mutableSetOf<Int>()

    fun submitList(newArticles: List<Article>) {
        articles = newArticles
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemArticleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(articles[position], position)
    }

    override fun getItemCount(): Int = articles.size

    inner class ViewHolder(
        private val binding: ItemArticleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: Article, position: Int) {
            binding.apply {
                // 设置图标背景色
                cardIcon.setCardBackgroundColor(Color.parseColor(article.iconBackgroundColor))

                // 设置卡片背景色
                root.setCardBackgroundColor(Color.parseColor(article.cardBackgroundColor))

                // 设置图标
                ivIcon.setImageResource(article.iconRes)

                tvTitle.text = article.title
                tvSubtitle.text = article.subtitle
                tvSummary.text = article.summary
                tvFullContent.text = article.body

                val titleColor: Int
                val subtitleColor: Int
                val summaryColor: Int
                val expandColor: Int
                when (article.id) {
                    "1" -> {
                        titleColor = Color.parseColor("#BE123C")
                        subtitleColor = Color.parseColor("#FB7185")
                        summaryColor = Color.parseColor("#E11D48")
                        expandColor = Color.parseColor("#F43F5E")
                    }
                    "2" -> {
                        titleColor = Color.parseColor("#4338CA")
                        subtitleColor = Color.parseColor("#818CF8")
                        summaryColor = Color.parseColor("#4F46E5")
                        expandColor = Color.parseColor("#6366F1")
                    }
                    "3" -> {
                        titleColor = Color.parseColor("#C2410C")
                        subtitleColor = Color.parseColor("#FB923C")
                        summaryColor = Color.parseColor("#EA580C")
                        expandColor = Color.parseColor("#F97316")
                    }
                    "4" -> {
                        titleColor = Color.parseColor("#0F766E")
                        subtitleColor = Color.parseColor("#2DD4BF")
                        summaryColor = Color.parseColor("#0D9488")
                        expandColor = Color.parseColor("#14B8A6")
                    }
                    "5" -> {
                        titleColor = Color.parseColor("#BE123C")
                        subtitleColor = Color.parseColor("#FB7185")
                        summaryColor = Color.parseColor("#E11D48")
                        expandColor = Color.parseColor("#F43F5E")
                    }
                    else -> {
                        titleColor = Color.parseColor("#BE123C")
                        subtitleColor = Color.parseColor("#FB7185")
                        summaryColor = Color.parseColor("#E11D48")
                        expandColor = Color.parseColor("#F43F5E")
                    }
                }
                tvTitle.setTextColor(titleColor)
                tvSubtitle.setTextColor(subtitleColor)
                tvSummary.setTextColor(summaryColor)

                // 展开状态
                val isExpanded = expandedPositions.contains(position)
                tvFullContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
                tvExpand.text = if (isExpanded) "收起" else "阅读完整科普"
                tvExpand.setTextColor(expandColor)
                tvExpand.visibility = if (article.body.isNotBlank()) View.VISIBLE else View.GONE

                // 点击展开/收起
                tvExpand.setOnClickListener {
                    if (isExpanded) {
                        expandedPositions.remove(position)
                    } else {
                        expandedPositions.add(position)
                    }
                    notifyItemChanged(position)
                }
            }
        }
    }
}
