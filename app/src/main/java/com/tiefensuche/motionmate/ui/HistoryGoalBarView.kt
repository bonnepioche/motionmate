/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tiefensuche.motionmate.R

/** Copper gradient progress, clamped at 100%; labels can show >100%. */
internal class HistoryGoalBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private var fraction: Float = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setFraction(value: Float) {
        val next = value.coerceIn(0f, 1f)
        if (fraction != next) {
            fraction = next
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val radius = h / 2f
        paint.shader = null
        paint.color = ContextCompat.getColor(context, R.color.bronze_surface_variant)
        canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, paint)
        val filled = w * fraction
        if (filled > 0f) {
            paint.shader = LinearGradient(0f, 0f, w, 0f,
                intArrayOf(0xFF9A5C33.toInt(), 0xFFE7AE78.toInt(), 0xFFBD804F.toInt()),
                null, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(RectF(0f, 0f, filled, h), radius, radius, paint)
            paint.shader = null
        }
    }
}
