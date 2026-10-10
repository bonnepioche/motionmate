/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.stats.ProgressIconOptions
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Ten scalable goal symbols; bronze fill is clamped, percentage text is not. */
internal class GoalProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private var style = ProgressIconOptions.DEFAULT
    private var fraction = 0f
    private var percent = 0
    private var animator: ValueAnimator? = null

    fun setIconStyle(value: String?) {
        val next = ProgressIconOptions.normalize(value)
        if (next != style) {
            style = next
            invalidate()
        }
    }

    fun setProgress(value: Float, wholePercent: Int) {
        percent = wholePercent.coerceAtLeast(0)
        val target = if (value.isNaN()) 0f else value.coerceIn(0f, 1f)
        animator?.cancel()
        if (windowToken == null || fraction == target) {
            fraction = target
            invalidate()
        } else {
            animator = ValueAnimator.ofFloat(fraction, target).apply {
                duration = 500L
                addUpdateListener {
                    fraction = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val side = minOf(width.toFloat(), height.toFloat()) - 3f
        if (side <= 0f) return
        canvas.save()
        canvas.translate((width - side) / 2f, (height - side) / 2f)
        canvas.scale(side / 100f, side / 100f)
        if (style == "05") drawCircularGauge(canvas)
        else drawFilledSymbol(canvas)
        canvas.restore()
    }

    private fun drawCircularGauge(canvas: Canvas) {
        val bounds = RectF(13f, 13f, 87f, 87f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 11f
        paint.color = color(R.color.bronze_surface_variant)
        canvas.drawArc(bounds, -90f, 360f, false, paint)
        paint.color = color(R.color.bronze_primary)
        if (fraction > 0f) canvas.drawArc(bounds, -90f, 360f * fraction, false, paint)
        paint.style = Paint.Style.FILL
        paint.color = color(R.color.bronze_on_surface)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        // The adjacent label retains the full percentage even beyond 999%.
        val display = "$percent%"
        paint.textSize = when {
            display.length >= 5 -> 19f
            display.length >= 4 -> 24f
            else -> 29f
        }
        canvas.drawText(display, 50f, 59f, paint)
    }

    private fun drawFilledSymbol(canvas: Canvas) {
        paint.color = color(R.color.bronze_surface_variant)
        drawSymbol(canvas)
        canvas.save()
        canvas.clipRect(0f, 0f, 100f * fraction, 100f)
        paint.color = color(R.color.bronze_primary)
        drawSymbol(canvas)
        canvas.restore()
        // The icon remains legible at 0% and in both bronze themes.
        paint.color = color(R.color.bronze_outline)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.4f
        drawSymbol(canvas)
        paint.style = Paint.Style.FILL
    }

    private fun drawSymbol(canvas: Canvas) {
        when (style) {
            "01" -> footprints(canvas)
            "02" -> walker(canvas)
            "03" -> windingPath(canvas)
            "04" -> mountain(canvas)
            "06" -> plant(canvas)
            "07" -> heart(canvas)
            "08" -> stairs(canvas)
            "09" -> flame(canvas)
            "10" -> star(canvas)
        }
    }

    private fun footprints(canvas: Canvas) {
        canvas.save()
        canvas.rotate(-20f, 34f, 55f)
        canvas.drawRoundRect(RectF(17f, 35f, 43f, 77f), 13f, 13f, paint)
        canvas.drawCircle(30f, 27f, 10f, paint)
        canvas.restore()
        canvas.save()
        canvas.rotate(20f, 68f, 48f)
        canvas.drawRoundRect(RectF(57f, 26f, 82f, 69f), 13f, 13f, paint)
        canvas.drawCircle(69f, 18f, 10f, paint)
        canvas.restore()
    }

    private fun walker(canvas: Canvas) {
        canvas.drawCircle(54f, 16f, 11f, paint)
        val previous = paint.style
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        val limbs = Path().apply {
            moveTo(53f, 33f); lineTo(45f, 56f); lineTo(60f, 71f); lineTo(73f, 91f)
            moveTo(44f, 56f); lineTo(21f, 87f)
            moveTo(49f, 43f); lineTo(26f, 55f)
            moveTo(49f, 42f); lineTo(72f, 51f)
        }
        canvas.drawPath(limbs, paint)
        paint.style = previous
    }

    private fun windingPath(canvas: Canvas) {
        val previous = paint.style
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 19f
        val road = Path().apply {
            moveTo(16f, 88f)
            cubicTo(92f, 88f, 3f, 55f, 57f, 50f)
            cubicTo(100f, 44f, 58f, 26f, 67f, 15f)
        }
        canvas.drawPath(road, paint)
        paint.style = previous
        canvas.drawCircle(69f, 14f, 11f, paint)
    }

    private fun mountain(canvas: Canvas) {
        val silhouette = Path().apply {
            moveTo(5f, 88f); lineTo(33f, 46f); lineTo(44f, 62f)
            lineTo(65f, 17f); lineTo(96f, 88f); close()
        }
        canvas.drawPath(silhouette, paint)
        val previous = paint.style
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawLine(65f, 17f, 65f, 4f, paint)
        paint.style = previous
        val flag = Path().apply {
            moveTo(65f, 4f); lineTo(83f, 9f); lineTo(65f, 14f); close()
        }
        canvas.drawPath(flag, paint)
    }

    private fun plant(canvas: Canvas) {
        val previous = paint.style
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        canvas.drawLine(50f, 90f, 50f, 29f, paint)
        canvas.drawLine(50f, 67f, 30f, 51f, paint)
        canvas.drawLine(50f, 48f, 68f, 30f, paint)
        paint.style = previous
        val left = Path().apply {
            moveTo(49f, 65f); cubicTo(13f, 67f, 12f, 28f, 8f, 21f)
            cubicTo(36f, 22f, 51f, 37f, 49f, 65f); close()
        }
        val right = Path().apply {
            moveTo(52f, 48f); cubicTo(48f, 22f, 67f, 11f, 92f, 7f)
            cubicTo(90f, 35f, 75f, 52f, 52f, 48f); close()
        }
        canvas.drawPath(left, paint)
        canvas.drawPath(right, paint)
        canvas.drawRoundRect(RectF(24f, 87f, 77f, 96f), 5f, 5f, paint)
    }

    private fun heart(canvas: Canvas) {
        val shape = Path().apply {
            moveTo(50f, 90f)
            cubicTo(37f, 76f, 9f, 53f, 9f, 32f)
            cubicTo(9f, 3f, 39f, 2f, 50f, 26f)
            cubicTo(65f, 2f, 91f, 6f, 91f, 32f)
            cubicTo(91f, 53f, 65f, 76f, 50f, 90f)
            close()
        }
        canvas.drawPath(shape, paint)
    }

    private fun stairs(canvas: Canvas) {
        val shape = Path().apply {
            moveTo(9f, 91f); lineTo(9f, 73f); lineTo(30f, 73f)
            lineTo(30f, 54f); lineTo(50f, 54f)
            lineTo(50f, 35f); lineTo(70f, 35f)
            lineTo(70f, 15f); lineTo(92f, 15f); lineTo(92f, 91f)
            close()
        }
        canvas.drawPath(shape, paint)
    }

    private fun flame(canvas: Canvas) {
        val shape = Path().apply {
            moveTo(50f, 93f)
            cubicTo(17f, 93f, 14f, 69f, 22f, 51f)
            cubicTo(29f, 38f, 39f, 31f, 39f, 14f)
            cubicTo(62f, 29f, 59f, 43f, 62f, 49f)
            cubicTo(74f, 42f, 75f, 32f, 74f, 26f)
            cubicTo(97f, 54f, 95f, 75f, 76f, 88f)
            cubicTo(68f, 94f, 56f, 95f, 50f, 93f); close()
        }
        canvas.drawPath(shape, paint)
    }

    private fun star(canvas: Canvas) {
        val shape = Path()
        for (i in 0..9) {
            val angle = -PI / 2.0 + i * PI / 5.0
            val radius = if (i % 2 == 0) 44.0 else 20.0
            val x = (50.0 + radius * cos(angle)).toFloat()
            val y = (50.0 + radius * sin(angle)).toFloat()
            if (i == 0) shape.moveTo(x, y) else shape.lineTo(x, y)
        }
        shape.close()
        canvas.drawPath(shape, paint)
    }

    private fun color(id: Int): Int = ContextCompat.getColor(context, id)

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
}
