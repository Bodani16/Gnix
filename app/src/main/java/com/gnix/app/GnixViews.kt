package com.gnix.app

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class GnixViews(private val context: Context) {
    companion object {
        val background = Color.rgb(13, 13, 15)
        val surface = Color.rgb(27, 27, 31)
        val raised = Color.rgb(37, 37, 42)
        val foreground = Color.rgb(250, 250, 250)
        val muted = Color.rgb(184, 184, 194)
        val red = Color.rgb(229, 57, 53)
        val border = Color.rgb(52, 52, 59)
    }
    fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
    fun column() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    fun row() = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    fun text(value: String, size: Float = 16f, color: Int = foreground, bold: Boolean = false) = TextView(context).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
        setLineSpacing(dp(3).toFloat(), 1f)
    }
    fun shape(color: Int, radius: Int = 22, outline: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (outline != null) setStroke(dp(1), outline)
    }
    fun card(color: Int = surface): LinearLayout = column().apply {
        background = shape(color); setPadding(dp(20), dp(20), dp(20), dp(20))
    }
    fun space(height: Int) = View(context).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    fun divider() = View(context).apply { setBackgroundColor(border); layoutParams = LinearLayout.LayoutParams(-1, dp(1)) }
    fun add(parent: LinearLayout, child: View, bottom: Int = 0) {
        parent.addView(child, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) })
    }
    fun clickable(view: View, radius: Int = 22, color: Int = surface, action: () -> Unit) {
        view.background = RippleDrawable(ColorStateList.valueOf(0x30FFFFFF), shape(color, radius), shape(Color.WHITE, radius))
        view.isClickable = true; view.isFocusable = true; view.setOnClickListener { action() }
    }
    fun primary(label: String, enabled: Boolean = true, action: () -> Unit) = text(label, 16f, background, true).apply {
        gravity = Gravity.CENTER; minHeight = dp(56)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        clickable(this, 18, red, action)
        isEnabled = enabled; alpha = if (enabled) 1f else .4f
    }
    fun secondary(label: String, action: () -> Unit) = text(label, 15f, foreground, true).apply {
        gravity = Gravity.CENTER; minHeight = dp(48); setPadding(dp(12), dp(12), dp(12), dp(12))
        clickable(this, 16, raised, action)
    }
    fun iconButton(kind: String, description: String, active: Boolean = false, action: () -> Unit) = FrameLayout(context).apply {
        contentDescription = description
        minimumWidth = dp(48); minimumHeight = dp(48)
        clickable(this, 16, if (active) raised else GnixViews.background, action)
        addView(GnixIcon(context, kind, if (active) red else GnixViews.foreground), FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER))
    }
    fun pill(label: String, selected: Boolean, action: () -> Unit) = text(label, 14f, if (selected) background else muted, selected).apply {
        gravity = Gravity.CENTER; minHeight = dp(48); setPadding(dp(19), dp(12), dp(19), dp(12))
        clickable(this, 24, if (selected) red else surface, action)
        contentDescription = "$label, ${if (selected) "selecionado" else "não selecionado"}"
    }
    fun switch(enabled: Boolean) = Switch(context).apply {
        isChecked = enabled; showText = false
        buttonTintList = ColorStateList.valueOf(red)
        thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(GnixViews.foreground, muted))
        trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(red, border))
        minWidth = dp(56); minHeight = dp(48)
    }
}

/** Original line icons, drawn locally without external assets. */
class GnixIcon(context: Context, private val kind: String, private val color: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.7f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save(); canvas.scale(width / 24f, height / 24f); paint.color = color
        fun line(x: Float, y: Float, x2: Float, y2: Float) = canvas.drawLine(x, y, x2, y2, paint)
        when (kind) {
            "bookmark" -> { val p = Path(); p.moveTo(6f, 3f); p.lineTo(18f, 3f); p.lineTo(18f, 21f); p.lineTo(12f, 17f); p.lineTo(6f, 21f); p.close(); canvas.drawPath(p, paint) }
            "search" -> { canvas.drawCircle(10f, 10f, 6f, paint); line(15f, 15f, 21f, 21f) }
            "back" -> { line(20f, 12f, 4f, 12f); line(4f, 12f, 10f, 6f); line(4f, 12f, 10f, 18f) }
            "refresh" -> { canvas.drawArc(RectF(4f, 4f, 20f, 20f), 40f, 285f, false, paint); line(20f, 4f, 20f, 10f); line(20f, 10f, 14f, 10f) }
            "settings" -> { line(3f, 6f, 21f, 6f); line(3f, 12f, 21f, 12f); line(3f, 18f, 21f, 18f); canvas.drawCircle(8f, 6f, 2f, paint); canvas.drawCircle(16f, 12f, 2f, paint); canvas.drawCircle(10f, 18f, 2f, paint) }
            "world" -> { canvas.drawCircle(12f, 12f, 9f, paint); canvas.drawOval(RectF(8f, 3f, 16f, 21f), paint); line(3f, 12f, 21f, 12f) }
            "tech" -> { canvas.drawRoundRect(RectF(6f, 6f, 18f, 18f), 2f, 2f, paint); canvas.drawRect(9f, 9f, 15f, 15f, paint); for (v in listOf(8f, 12f, 16f)) { line(v, 2f, v, 5f); line(v, 19f, v, 22f); line(2f, v, 5f, v); line(19f, v, 22f, v) } }
            "economy" -> { line(4f, 3f, 4f, 21f); line(4f, 21f, 21f, 21f); line(7f, 16f, 12f, 10f); line(12f, 10f, 16f, 13f); line(16f, 13f, 21f, 5f) }
            "health" -> { canvas.drawRoundRect(RectF(3f, 3f, 21f, 21f), 5f, 5f, paint); line(12f, 7f, 12f, 17f); line(7f, 12f, 17f, 12f) }
            "sports" -> { canvas.drawCircle(12f, 12f, 9f, paint); val p = Path(); p.moveTo(12f, 7f); p.lineTo(17f, 10f); p.lineTo(15f, 16f); p.lineTo(9f, 16f); p.lineTo(7f, 10f); p.close(); canvas.drawPath(p, paint) }
            "check" -> { line(4f, 12f, 9f, 17f); line(9f, 17f, 20f, 6f) }
            "arrow" -> { line(4f, 12f, 20f, 12f); line(14f, 6f, 20f, 12f); line(20f, 12f, 14f, 18f) }
            "plus" -> { line(12f, 4f, 12f, 20f); line(4f, 12f, 20f, 12f) }
            else -> { canvas.drawRoundRect(RectF(4f, 3f, 20f, 21f), 2f, 2f, paint); canvas.drawRect(7f, 7f, 11f, 12f, paint); line(14f, 7f, 17f, 7f); line(14f, 11f, 17f, 11f); line(7f, 16f, 17f, 16f) }
        }
        canvas.restore()
    }
}
