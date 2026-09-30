package com.gnix.app

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.TextUtils
import java.text.SimpleDateFormat
import java.util.*

/** Keeps only visible article cards in memory, recycling native Android views. */
class ArticleAdapter(
    private val context: Context,
    private val ui: GnixViews,
    private val sourceFor: (String) -> Source?,
    private val isSaved: (String) -> Boolean,
    private val open: (Article) -> Unit,
    private val toggleSaved: (Article) -> Unit
) : BaseAdapter() {
    private var articles = emptyList<Article>()
    private val dateFormat = SimpleDateFormat("dd MMM", Locale("pt", "BR"))
    fun submit(values: List<Article>) { articles = values; notifyDataSetChanged() }
    override fun getCount(): Int = articles.size
    override fun getItem(position: Int): Article = articles[position]
    override fun getItemId(position: Int): Long = position.toLong()

    private class Holder(
        val outer: LinearLayout, val metadata: TextView, val sourceIcon: FrameLayout,
        val headline: TextView, val summary: TextView, val read: TextView, val bookmark: FrameLayout
    )
    private fun create(): Holder {
        val outer = ui.column().apply { setPadding(ui.dp(22), 0, ui.dp(22), ui.dp(14)) }
        val card = ui.card()
        val metadataRow = ui.row()
        val sourceIcon = FrameLayout(context)
        metadataRow.addView(sourceIcon, LinearLayout.LayoutParams(ui.dp(15), ui.dp(15)).apply { rightMargin = ui.dp(8) })
        val metadata = ui.text("", 12f, GnixViews.muted).apply { maxLines = 2 }
        metadataRow.addView(metadata, LinearLayout.LayoutParams(0, -2, 1f))
        ui.add(card, metadataRow, 13)
        val headline = ui.text("", 19f, bold = true).apply { isFocusable = true }
        ui.add(card, headline, 10)
        val summary = ui.text("", 15f, GnixViews.muted).apply { maxLines = 4; ellipsize = TextUtils.TruncateAt.END }
        ui.add(card, summary, 13)
        val actions = ui.row()
        val read = ui.text("Ler na fonte  ↗", 12f, GnixViews.red, true).apply {
            minHeight = ui.dp(48); gravity = Gravity.CENTER_VERTICAL; isFocusable = true
        }
        actions.addView(read, LinearLayout.LayoutParams(0, -2, 1f))
        val bookmark = FrameLayout(context).apply { isFocusable = true; minimumHeight = ui.dp(48); minimumWidth = ui.dp(48) }
        actions.addView(bookmark, LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)))
        card.addView(actions); ui.add(outer, card)
        return Holder(outer, metadata, sourceIcon, headline, summary, read, bookmark).also { outer.tag = it }
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder = (convertView?.tag as? Holder) ?: create()
        val article = articles[position]
        val source = sourceFor(article.sourceId)
        holder.metadata.text = "${source?.name ?: "Fonte"} · ${relativeTime(article.publishedAt)}"
        val kind = when (source?.category) {
            "Mundo" -> "world"; "Economia" -> "economy"; "Tecnologia" -> "tech"
            "Esportes" -> "sports"; "Saúde" -> "health"; else -> "news"
        }
        holder.sourceIcon.removeAllViews()
        holder.sourceIcon.addView(GnixIcon(context, kind, GnixViews.muted), FrameLayout.LayoutParams(-1, -1))
        holder.headline.text = article.title
        holder.headline.setOnClickListener { open(article) }
        holder.summary.text = article.summary
        holder.summary.visibility = if (article.summary.isEmpty()) View.GONE else View.VISIBLE
        holder.summary.setOnClickListener { open(article) }
        holder.read.contentDescription = "Ler ${article.title} na fonte"
        holder.read.setOnClickListener { open(article) }
        val bookmarked = isSaved(article.url)
        holder.bookmark.contentDescription = if (bookmarked) "Remover dos salvos: ${article.title}" else "Salvar notícia: ${article.title}"
        holder.bookmark.removeAllViews()
        holder.bookmark.addView(GnixIcon(context, "bookmark", if (bookmarked) GnixViews.red else GnixViews.foreground), FrameLayout.LayoutParams(ui.dp(23), ui.dp(23), Gravity.CENTER))
        ui.clickable(holder.bookmark, 16, if (bookmarked) GnixViews.raised else GnixViews.surface) { toggleSaved(article) }
        return holder.outer
    }
    private fun relativeTime(timestamp: Long): String {
        if (timestamp <= 0) return "sem data"
        val minutes = ((System.currentTimeMillis() - timestamp) / 60000).coerceAtLeast(0)
        return when {
            minutes == 0L -> "agora"; minutes < 60 -> "há $minutes min"
            minutes < 1440 -> "há ${minutes / 60} h"; else -> dateFormat.format(Date(timestamp))
        }
    }
}
