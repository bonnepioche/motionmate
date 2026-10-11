/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Matrix
import androidx.core.graphics.ColorUtils
import android.graphics.PathMeasure
import android.graphics.DashPathEffect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.stats.ProgressIconOptions
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow

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

    /**
     * Icon 05: a slim brushed-copper ring. The angular SweepGradient follows the
     * circle rather than cutting straight across it as a LinearGradient would.
     * The full track stays visible at 0%; only the completed arc is metallic.
     */
    private fun drawCircularGauge(canvas: Canvas) {
        val bounds = RectF(13.5f, 13.5f, 86.5f, 86.5f)
        val startAngle = -90f
        val sweep = 360f * fraction.coerceIn(0f, 1f)
        val bronze = color(R.color.bronze_primary)
        val copperDark = ColorUtils.blendARGB(bronze, Color.BLACK, .31f)
        val copperMid = ColorUtils.blendARGB(bronze, color(R.color.bronze_tertiary), .16f)
        val copperLight = ColorUtils.blendARGB(bronze, Color.WHITE, .34f)
        val copperGlint = ColorUtils.blendARGB(bronze, Color.WHITE, .61f)

        paint.shader = null
        paint.pathEffect = null
        paint.alpha = 255
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND

        // Soft, slim neutral track, with a quieter center line.
        paint.strokeWidth = 8.2f  // previously 11f
        paint.color = ColorUtils.blendARGB(
            color(R.color.bronze_surface_variant), color(R.color.bronze_outline), .37f
        )
        canvas.drawArc(bounds, startAngle, 360f, false, paint)
        paint.strokeWidth = 5.5f
        paint.color = color(R.color.bronze_surface_variant)
        canvas.drawArc(bounds, startAngle, 360f, false, paint)

        if (sweep > 0f) {
            // Underlying darker rim provides physical depth without a thick border.
            paint.strokeWidth = 8.6f
            paint.color = copperDark
            canvas.drawArc(bounds, startAngle, sweep, false, paint)

            // Angular copper highlights go around the ring, including at 100%.
            val copperSweep = SweepGradient(
                50f, 50f,
                intArrayOf(
                    copperMid, copperLight, copperGlint, copperLight,
                    bronze, copperDark, copperMid, copperLight, copperMid
                ),
                floatArrayOf(0f, .13f, .24f, .29f, .42f, .60f, .72f, .85f, 1f)
            )
            copperSweep.setLocalMatrix(Matrix().apply {
                setRotate(-90f, 50f, 50f)
            })
            paint.shader = copperSweep
            paint.strokeWidth = 7.2f
            paint.color = Color.WHITE
            canvas.drawArc(bounds, startAngle, sweep, false, paint)
            paint.shader = null

            // Thin inner glint creates a satin, raised-metal effect.
            paint.strokeWidth = 1.15f
            paint.color = copperGlint
            paint.alpha = 125
            canvas.drawArc(RectF(16.8f, 16.8f, 83.2f, 83.2f), startAngle, sweep, false, paint)
            paint.alpha = 255
        }

        // Keep text comfortably inside the ring, even for values beyond 100%.
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = color(R.color.bronze_on_surface)
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create(
            "sans-serif-medium", android.graphics.Typeface.BOLD
        )
        val display = "$percent%"
        paint.textSize = when {
            display.length >= 6 -> 16f
            display.length >= 5 -> 18f
            display.length >= 4 -> 21f  // 103%: previously 24f
            else -> 25f
        }
        val measuredWidth = paint.measureText(display)
        if (measuredWidth > 51f) {
            paint.textSize *= 51f / measuredWidth
        }
        val baseline = 50f - (paint.ascent() + paint.descent()) / 2f
        canvas.drawText(display, 50f, baseline, paint)

        // The shared Paint is reused by the other nine icons.
        paint.shader = null
        paint.alpha = 255
    }
    /**
     * Icons 01, 02, 04 and 06..10: brushed copper, using the SAME geometry as
     * before. 03 already has a perspective-copper shader, and 05 its SweepGradient.
     * The copper finish is clipped to the existing progress fraction, which
     * preserves partial filling in both the dashboard and Settings previews.
     */
    private fun drawFilledSymbol(canvas: Canvas) {
        if (style == "03") {
            drawWindingProgress(canvas)
            return
        }

        // Reset the shared Paint to avoid shader/style leakage between icons.
        paint.shader = null
        paint.pathEffect = null
        paint.alpha = 255
        paint.style = Paint.Style.FILL

        // Neutral track: uncompleted portions remain visible but understated.
        paint.color = color(R.color.bronze_surface_variant)
        drawSymbol(canvas)

        if (fraction > 0f) {
            val bronze = color(R.color.bronze_primary)
            val tertiary = color(R.color.bronze_tertiary)
            val copperLight = ColorUtils.blendARGB(bronze, Color.WHITE, .54f)
            val copperGlint = ColorUtils.blendARGB(bronze, Color.WHITE, .74f)
            val copperWarm = ColorUtils.blendARGB(bronze, tertiary, .22f)
            val copperDark = ColorUtils.blendARGB(bronze, Color.BLACK, .24f)

            canvas.save()
            // Keep the established LEFT-TO-RIGHT progress and its 500 ms animation.
            canvas.clipRect(0f, 0f, 100f * fraction.coerceIn(0f, 1f), 100f)

            // Brushed-copper shading: directional, with a distinct soft highlight
            // and shaded underside, matching the ring's bright/dark variations.
            paint.shader = LinearGradient(
                7f, 6f, 93f, 94f,
                intArrayOf(
                    copperGlint, copperLight, copperWarm, copperDark,
                    ColorUtils.blendARGB(bronze, Color.WHITE, .18f)
                ),
                floatArrayOf(0f, .19f, .46f, .79f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.color = Color.WHITE
            drawSymbol(canvas)
            paint.shader = null

            // A low-opacity satin reflection adds some depth to rounded forms,
            // but is confined to the completed portion of the SAME symbol.
            paint.shader = LinearGradient(
                5f, 10f, 86f, 72f,
                intArrayOf(copperGlint, copperLight, copperDark),
                floatArrayOf(0f, .31f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.alpha = 40
            drawSymbol(canvas)
            paint.shader = null
            paint.alpha = 255
            canvas.restore()
        }

        // Preserve the established outlines, including the walking figure's
        // stroke-based silhouette and the mountain flag.
        paint.color = color(R.color.bronze_outline)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.4f
        drawSymbol(canvas)
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.alpha = 255
    }
    private fun drawSymbol(canvas: Canvas) {
        when (style) {
            "01" -> footprints(canvas)
            "02" -> walker(canvas)
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

    /**
     * Icon 03: a glossy, flared perspective ribbon leading toward a destination flag.
     * All geometry stays within the shared 100 x 100 viewport, including previews.
     */
    private val windingRoadCenterline = Path().apply {
        moveTo(20f, 80f)
        cubicTo(53f, 88f, 87f, 80f, 67f, 66f)
        cubicTo(48f, 54f, 34f, 55f, 56f, 43f)
        cubicTo(76f, 36f, 54f, 34f, 68f, 26f)
    }

    // Cache the geometry once: only the paint/clip changes during animations.
    private val windingRoadRibbon: Path by lazy {
        val measure = PathMeasure(windingRoadCenterline, false)
        val point = FloatArray(2)
        val tangent = FloatArray(2)
        val left = ArrayList<Pair<Float, Float>>(97)
        val right = ArrayList<Pair<Float, Float>>(97)
        val samples = 96
        for (i in 0..samples) {
            val progress = i.toFloat() / samples
            measure.getPosTan(measure.length * progress, point, tangent)
            // Ribbon perspective: open, broad foreground and narrow far end.
            val halfWidth = 2.8f + 9.6f * (1f - progress).pow(2.7f)
            left.add(Pair(point[0] - tangent[1] * halfWidth,
                          point[1] + tangent[0] * halfWidth))
            right.add(Pair(point[0] + tangent[1] * halfWidth,
                           point[1] - tangent[0] * halfWidth))
        }
        Path().apply {
            moveTo(left[0].first, left[0].second)
            for (i in 1 until left.size) lineTo(left[i].first, left[i].second)
            for (i in right.indices.reversed()) lineTo(right[i].first, right[i].second)
            close()
        }
    }

    /** Fine reflective line just inside the road's left-hand edge. */
    private val windingRoadHighlight: Path by lazy {
        val measure = PathMeasure(windingRoadCenterline, false)
        val point = FloatArray(2)
        val tangent = FloatArray(2)
        Path().apply {
            for (i in 0..96) {
                val progress = i / 96f
                measure.getPosTan(measure.length * progress, point, tangent)
                val offset = (2.8f + 9.6f * (1f - progress).pow(2.7f)) - 1.7f
                val x = point[0] - tangent[1] * offset
                val y = point[1] + tangent[0] * offset
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
    }

    private val windingFlag = Path().apply {
        moveTo(68f, 10.5f)
        lineTo(84f, 14.2f)
        lineTo(68f, 18.2f)
        close()
    }

    // Open, hairline grass strokes rather than filled triangular silhouettes.
    private val windingGrassLeft = Path().apply {
        moveTo(17f, 64f); cubicTo(14f, 62f, 12f, 58f, 12f, 56f)
        moveTo(17f, 64f); cubicTo(16f, 61f, 16f, 56f, 17f, 54f)
        moveTo(17f, 64f); cubicTo(19f, 61f, 21f, 59f, 23f, 58f)
    }

    private val windingGrassRight = Path().apply {
        moveTo(85f, 73f); cubicTo(82f, 71f, 82f, 68f, 82f, 65f)
        moveTo(85f, 73f); cubicTo(85f, 70f, 87f, 67f, 88f, 65f)
        moveTo(85f, 73f); cubicTo(88f, 70f, 90f, 69f, 92f, 68f)
    }

    /**
     * Special rendering only for icon 03. Other icons keep drawFilledSymbol().
     * The existing fraction still clips the bronze finish from left to right.
     */
    private fun drawWindingProgress(canvas: Canvas) {
        val base = color(R.color.bronze_surface_variant)
        val primary = color(R.color.bronze_primary)
        val outline = color(R.color.bronze_outline)

        paint.shader = null
        paint.pathEffect = null
        paint.style = Paint.Style.FILL

        // Soft offset shadow; deliberately subtle, not a thick border.
        paint.color = color(R.color.bronze_on_surface)
        paint.alpha = 30
        canvas.save()
        canvas.translate(1f, 1.9f)
        canvas.drawPath(windingRoadRibbon, paint)
        canvas.restore()
        paint.alpha = 255

        // Uncompleted part stays visible in the neutral palette.
        paint.color = base
        canvas.drawPath(windingRoadRibbon, paint)
        paint.color = outline
        canvas.drawRoundRect(RectF(67.5f, 10.5f, 68.8f, 26.5f), .65f, .65f, paint)
        canvas.drawPath(windingFlag, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.3f
        canvas.drawPath(windingGrassLeft, paint)
        canvas.drawPath(windingGrassRight, paint)
        paint.style = Paint.Style.FILL

        // Subtle outline on the base, much thinner than dev.10.1.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = .4f
        paint.color = outline
        paint.alpha = 95
        canvas.drawPath(windingRoadRibbon, paint)
        paint.alpha = 255

        canvas.save()
        canvas.clipRect(0f, 0f, 100f * fraction, 100f)
        paint.style = Paint.Style.FILL

        // Three-stop bronze gradient, adjusted automatically for light/dark mode.
        paint.shader = LinearGradient(
            17f, 89f, 76f, 17f,
            intArrayOf(
                ColorUtils.blendARGB(primary, Color.WHITE, .34f),
                ColorUtils.blendARGB(primary, color(R.color.bronze_tertiary), .16f),
                ColorUtils.blendARGB(primary, Color.BLACK, .16f)
            ),
            floatArrayOf(0f, .54f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.color = Color.WHITE
        canvas.drawPath(windingRoadRibbon, paint)
        paint.shader = null

        // Gentle reflective rim on the road, rather than a heavy outline.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.7f
        paint.color = ColorUtils.blendARGB(primary, Color.WHITE, .64f)
        paint.alpha = 170
        canvas.drawPath(windingRoadHighlight, paint)
        paint.alpha = 255

        // Fine, centered dashes that follow the same S-curve as the bronze ribbon.
        // The existing completed-progress clip applies to the marks as well.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.15f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = ColorUtils.blendARGB(primary, Color.WHITE, .76f)
        paint.alpha = 177
        paint.pathEffect = DashPathEffect(floatArrayOf(2.1f, 5.2f), 1f)
        canvas.drawPath(windingRoadCenterline, paint)
        paint.pathEffect = null
        paint.alpha = 255
        paint.style = Paint.Style.FILL

        // Delicate destination flag and grass, not chunky silhouettes.
        paint.color = ColorUtils.blendARGB(primary, Color.WHITE, .16f)
        canvas.drawRoundRect(RectF(67.5f, 10.5f, 68.8f, 26.5f), .65f, .65f, paint)
        canvas.drawPath(windingFlag, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.3f
        paint.color = ColorUtils.blendARGB(primary, Color.WHITE, .20f)
        canvas.drawPath(windingGrassLeft, paint)
        canvas.drawPath(windingGrassRight, paint)
        paint.style = Paint.Style.FILL
        canvas.restore()

        // Leave the shared paint ready for the other icons on future frames.
        paint.shader = null
        paint.pathEffect = null
        paint.alpha = 255
        paint.style = Paint.Style.FILL
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
