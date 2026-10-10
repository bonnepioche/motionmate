/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.tiefensuche.motionmate.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tiefensuche.motionmate.R

/** A lightweight bronze sneaker that fills from heel to toe as steps increase. */
internal class ShoeProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.6f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val solid = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val detail = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.1f
        strokeCap = Paint.Cap.ROUND
    }

    private val sneaker = Path().apply {
        // Profile of a walking shoe: heel, high quarter, laces and rounded toe.
        moveTo(8f, 37f)
        quadTo(10f, 33f, 19f, 32f)
        quadTo(30f, 30f, 34f, 21f)
        lineTo(39f, 11f)
        quadTo(42f, 8f, 46f, 13f)
        lineTo(60f, 28f)
        quadTo(66f, 31f, 78f, 33f)
        lineTo(91f, 37f)
        quadTo(97f, 40f, 96f, 46f)
        quadTo(96f, 50f, 89f, 50f)
        lineTo(16f, 50f)
        quadTo(8f, 50f, 8f, 44f)
        close()
    }

    private var fillFraction = 0f
    private var animator: ValueAnimator? = null

    fun setProgress(value: Float) {
        val target = value.coerceIn(0f, 1f)
        if (target == fillFraction) return
        animator?.cancel()
        // The initial value is drawn directly before the view is attached.
        if (windowToken == null) {
            fillFraction = target
            invalidate()
            return
        }
        animator = ValueAnimator.ofFloat(fillFraction, target).apply {
            duration = 500L
            addUpdateListener {
                fillFraction = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Fixed drawing coordinates: 100 x 56, keeping the shoe proportions.
        val factor = minOf((w - 4f) / 100f, (h - 4f) / 56f)
        canvas.save()
        canvas.translate((w - 100f * factor) / 2f, (h - 56f * factor) / 2f)
        canvas.scale(factor, factor)

        solid.color = ContextCompat.getColor(context, R.color.bronze_surface_variant)
        canvas.drawPath(sneaker, solid)

        solid.color = ContextCompat.getColor(context, R.color.bronze_primary)
        canvas.save()
        canvas.clipRect(0f, 0f, 100f * fillFraction, 56f)
        canvas.drawPath(sneaker, solid)
        canvas.restore()

        stroke.color = ContextCompat.getColor(context, R.color.bronze_primary)
        canvas.drawPath(sneaker, stroke)

        // Contrasting seams and shoe laces remain visible across fill levels.
        detail.color = ContextCompat.getColor(context, R.color.bronze_on_surface_variant)
        canvas.drawLine(13f, 44f, 94f, 44f, detail)
        canvas.drawLine(47f, 22f, 55f, 19f, detail)
        canvas.drawLine(51f, 27f, 60f, 24f, detail)
        canvas.drawLine(56f, 31f, 65f, 28f, detail)
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
}
