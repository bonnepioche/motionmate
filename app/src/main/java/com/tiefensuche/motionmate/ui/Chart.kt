/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.stats.WeeklyStepResult
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight weekly chart: seven rounded copper-gradient columns and a dashed goal.
 * No pan/zoom: this chart is part of a vertically scrolling dashboard.
 */
internal class Chart : View {
    private var week: WeeklyStepResult? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    internal fun setWeek(result: WeeklyStepResult) {
        week = result
        val formatter = NumberFormat.getIntegerInstance()
        contentDescription = context.getString(
            R.string.week_accessibility,
            formatter.format(result.total),
            formatter.format(result.averagePerElapsedDay),
            result.achievedDays,
            result.elapsedDays
        )
        invalidate()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity

    private fun mix(from: Int, to: Int, fraction: Float): Int {
        val t = fraction.coerceIn(0f, 1f)
        fun channel(a: Int, b: Int) = (a + (b - a) * t).toInt().coerceIn(0, 255)
        return Color.rgb(channel(Color.red(from), Color.red(to)),
            channel(Color.green(from), Color.green(to)),
            channel(Color.blue(from), Color.blue(to)))
    }

    private fun pretty(steps: Long): String = when {
        steps >= 1_000_000L -> String.format(Locale.getDefault(), "%.1fM", steps / 1_000_000.0)
        steps >= 10_000L -> String.format(Locale.getDefault(), "%.0fk", steps / 1000.0)
        steps >= 1_000L -> String.format(Locale.getDefault(), "%.1fk", steps / 1000.0)
        else -> steps.toString()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val result = week ?: return
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= dp(65f)) return

        val bronze = ContextCompat.getColor(context, R.color.bronze_chart_bar)
        val foreground = ContextCompat.getColor(context, R.color.bronze_on_surface_variant)
        val outline = ContextCompat.getColor(context, R.color.bronze_outline)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val startX = dp(7f)
        val endX = width - dp(7f)
        val top = dp(22f)
        val bottom = height - dp(29f)
        if (bottom <= top) return
        val range = bottom - top
        val maximum = max(result.goal.toLong(), result.days.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val axisMax = maximum * 1.19f
        val slot = (endX - startX) / 7f
        val barWidth = slot * 0.68f

        paint.color = mix(outline, Color.TRANSPARENT, 0.12f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(0.8f)
        canvas.drawLine(startX, bottom, endX, bottom, paint)

        val days = Array(7) { "" }
        val cursor = Calendar.getInstance().apply { timeInMillis = result.weekStart }
        for (index in 0..6) {
            days[index] = cursor.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT,
                Locale.getDefault())?.removeSuffix(".")?.take(3) ?: "${index + 1}"
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }

        result.days.forEachIndexed { index, steps ->
            val centerX = startX + slot * (index + 0.5f)
            val available = index < result.elapsedDays
            val barTop = bottom - range * min(steps.toFloat() / axisMax, 1f)
            if (available && steps > 0L && barTop < bottom) {
                val rect = RectF(centerX - barWidth / 2f, barTop,
                    centerX + barWidth / 2f, bottom)
                // Three-stop vertical gradient creates a soft metallic sheen.
                val topColor = mix(bronze, Color.WHITE, if (steps >= result.goal) 0.35f else 0.20f)
                val midColor = bronze
                val bottomColor = mix(bronze, Color.BLACK, 0.27f)
                paint.style = Paint.Style.FILL
                paint.shader = LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                    intArrayOf(topColor, midColor, bottomColor),
                    floatArrayOf(0f, 0.43f, 1f), Shader.TileMode.CLAMP)
                canvas.drawRoundRect(rect, dp(4f), dp(4f), paint)
                paint.shader = null
                // Narrow vertical glint on the left side of each column.
                paint.color = mix(topColor, Color.WHITE, 0.18f)
                paint.alpha = 95
                canvas.drawRoundRect(RectF(rect.left + dp(1.2f), rect.top + dp(2f),
                    rect.left + dp(2.6f), rect.bottom - dp(2f)), dp(1f), dp(1f), paint)
                paint.alpha = 255
            }

            paint.style = Paint.Style.FILL
            paint.shader = null
            paint.color = foreground
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = sp(9.5f)
            if (available) {
                val label = pretty(steps)
                val labelY = max(top - dp(3f), barTop - dp(5f))
                canvas.drawText(label, centerX, labelY, paint)
            }
            paint.textSize = sp(10f)
            if (!available) paint.alpha = 115
            canvas.drawText(days[index], centerX, height - dp(6f), paint)
            paint.alpha = 255
            if (index == result.todayIndex) {
                paint.color = bronze
                canvas.drawCircle(centerX, height - dp(1.6f), dp(1.6f), paint)
            }
        }

        // Draw above the bars so the target stays visible even on successful days.
        val goalY = bottom - range * (result.goal / axisMax)
        paint.style = Paint.Style.STROKE
        paint.shader = null
        paint.color = mix(bronze, foreground, 0.22f)
        paint.alpha = 200
        paint.strokeWidth = dp(1.2f)
        paint.pathEffect = DashPathEffect(floatArrayOf(dp(5f), dp(4f)), 0f)
        canvas.drawLine(startX, goalY, endX, goalY, paint)
        paint.pathEffect = null
        paint.alpha = 255
    }
}
