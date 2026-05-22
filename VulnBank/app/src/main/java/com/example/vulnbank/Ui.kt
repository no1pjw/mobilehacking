package com.example.vulnbank

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

object Palette {
    const val Ink = 0xFF14213D.toInt()
    const val Muted = 0xFF5C677D.toInt()
    const val Paper = 0xFFF8FAFC.toInt()
    const val Card = Color.WHITE
    const val Line = 0xFFE2E8F0.toInt()
    const val Blue = 0xFF2563EB.toInt()
    const val Green = 0xFF059669.toInt()
    const val Red = 0xFFDC2626.toInt()
    const val Amber = 0xFFD97706.toInt()
}

fun Context.screenRoot(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(18), dp(24), dp(18), dp(24))
    setBackgroundColor(Palette.Paper)
}

fun Context.title(text: String): TextView = TextView(this).apply {
    this.text = text
    textSize = 28f
    setTextColor(Palette.Ink)
    typeface = Typeface.DEFAULT_BOLD
}

fun Context.subtitle(text: String): TextView = TextView(this).apply {
    this.text = text
    textSize = 14f
    setTextColor(Palette.Muted)
    setPadding(0, dp(4), 0, dp(16))
}

fun Context.section(text: String): TextView = TextView(this).apply {
    this.text = text
    textSize = 18f
    setTextColor(Palette.Ink)
    typeface = Typeface.DEFAULT_BOLD
    setPadding(0, dp(18), 0, dp(8))
}

fun Context.card(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(16), dp(14), dp(16), dp(14))
    background = GradientDrawable().apply {
        setColor(Palette.Card)
        cornerRadius = dp(8).toFloat()
        setStroke(1, Palette.Line)
    }
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins(0, 0, 0, dp(10))
    }
    elevation = dp(2).toFloat()
}

fun Context.accentCard(color: Int): LinearLayout = card().apply {
    background = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(8).toFloat()
    }
}

fun Context.primaryButton(text: String): Button = Button(this).apply {
    this.text = text
    setTextColor(Color.WHITE)
    setBackgroundColor(Palette.Blue)
    minHeight = dp(48)
}

fun Context.secondaryButton(text: String): Button = Button(this).apply {
    this.text = text
    setTextColor(Palette.Ink)
    minHeight = dp(44)
}

fun Context.metric(label: String, value: String, color: Int = Palette.Ink): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val labelView = TextView(this@metric).apply {
            text = label
            textSize = 12f
            setTextColor(Palette.Muted)
        }
        val valueView = TextView(this@metric).apply {
            text = value
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color)
        }
        addView(labelView)
        addView(valueView)
    }
}

fun View.withTopMargin(context: Context, margin: Int): View = apply {
    layoutParams = (layoutParams as? LinearLayout.LayoutParams ?: LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )).apply {
        setMargins(leftMargin, context.dp(margin), rightMargin, bottomMargin)
    }
}

fun LinearLayout.animateChildren() {
    for (index in 0 until childCount) {
        val child = getChildAt(index)
        child.alpha = 0f
        child.translationY = 28f
        child.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(index * 55L)
            .setDuration(260L)
            .start()
    }
}

fun Context.centerText(text: String, size: Float = 14f): TextView = TextView(this).apply {
    this.text = text
    textSize = size
    gravity = Gravity.CENTER
    setTextColor(Palette.Muted)
}
