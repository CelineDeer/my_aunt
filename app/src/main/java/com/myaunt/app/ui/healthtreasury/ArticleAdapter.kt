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

                // 设置标题和副标题
                tvTitle.text = article.title
                tvTitle.setTextColor(Color.parseColor(if (article.category.name == "HEALTH_DIET") "#E91E63" else "#5C6BC0"))

                tvSubtitle.text = article.subtitle
                tvSummary.text = article.summary

                // 展开状态
                val isExpanded = expandedPositions.contains(position)
                tvFullContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
                tvExpand.text = if (isExpanded) "收起" else "阅读完整科普"

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
