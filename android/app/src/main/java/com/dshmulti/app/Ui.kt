package com.dshmulti.app

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.TextView

object Ui {
    val BG = 0xFF0E1116.toInt()
    val BAR = 0xFF141922.toInt()
    val CARD = 0xFF181E28.toInt()
    val STROKE = 0xFF262E3B.toInt()
    val TEXT = 0xFFE8ECF3.toInt()
    val SUB = 0xFF8B94A6.toInt()
    val ACCENT = 0xFF4F8CFF.toInt()
    val OK = 0xFF33C08A.toInt()
    val ERR = 0xFFF0616D.toInt()

    fun dp(c: Context, v: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), c.resources.displayMetrics).toInt()

    fun round(c: Context, radius: Int, color: Int, strokeColor: Int = 0, strokeWidth: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(c, radius).toFloat()
            setColor(color)
            if (strokeWidth > 0 && strokeColor != 0) setStroke(dp(c, strokeWidth), strokeColor)
        }

    fun text(c: Context, s: CharSequence, sp: Float, color: Int, bold: Boolean = false): TextView =
        TextView(c).apply {
            text = s
            setTextColor(color)
            textSize = sp
            includeFontPadding = false
            if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
}
