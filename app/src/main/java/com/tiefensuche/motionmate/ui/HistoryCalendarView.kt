/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.stats.DailyHistorySummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Accessible month grid with visual day status; never modifies step history. */
internal class HistoryCalendarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    var onDaySelected: ((Long) -> Unit)? = null

    private var selectedDay = DailyHistorySummary.startOfDay(System.currentTimeMillis())
    private var minDay = selectedDay
    private var maxDay = selectedDay
    private var month = Calendar.getInstance().apply {
        timeInMillis = selectedDay
        set(Calendar.DAY_OF_MONTH, 1)
    }
    private var daily: Map<Long, Long> = emptyMap()
    private var todaySteps = 0
    private var goal = 10000
    private val heading = TextView(context)
    private val previousMonth = TextView(context)
    private val nextMonth = TextView(context)
    private val cells = ArrayList<LinearLayout>(42)
    private val dayNumbers = ArrayList<TextView>(42)
    private val dots = ArrayList<View>(42)
    private val accent get() = ContextCompat.getColor(context, R.color.bronze_primary)
    private val normal get() = ContextCompat.getColor(context, R.color.bronze_on_surface)
    private val subdued get() = ContextCompat.getColor(context, R.color.bronze_on_surface_variant)
    private val reached get() = ContextCompat.getColor(context, R.color.history_goal_met)
    private val below get() = ContextCompat.getColor(context, R.color.history_goal_missed)
    private val empty get() = ContextCompat.getColor(context, R.color.history_no_data)

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density + 0.5f).toInt()
    private fun shape(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun noDataRing(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.TRANSPARENT)
        setStroke(dp(1), empty)
    }

    init {
        orientation = VERTICAL
        val top = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        fun arrow(symbol: String, button: TextView) {
            button.text = symbol
            button.textSize = 23f
            button.gravity = Gravity.CENTER
            button.setTextColor(accent)
            button.isClickable = true
            button.isFocusable = true
            top.addView(button, LayoutParams(dp(44), dp(40)))
        }
        arrow("‹", previousMonth)
        heading.gravity = Gravity.CENTER
        heading.textSize = 15f
        heading.setTextColor(normal)
        top.addView(heading, LayoutParams(0, dp(40), 1f))
        arrow("›", nextMonth)
        addView(top, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        previousMonth.setOnClickListener { shiftMonth(-1) }
        nextMonth.setOnClickListener { shiftMonth(1) }
        previousMonth.contentDescription = context.getString(R.string.history_previous_month)
        nextMonth.contentDescription = context.getString(R.string.history_next_month)

        val weekdays = LinearLayout(context)
        val start = Calendar.getInstance().firstDayOfWeek
        for (index in 0..6) {
            val dayOfWeek = (start - 1 + index) % 7 + 1
            val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dayOfWeek) }
            val label = TextView(context).apply {
                text = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                    ?.removeSuffix(".")?.take(3) ?: "·"
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(subdued)
            }
            weekdays.addView(label, LayoutParams(0, dp(23), 1f))
        }
        addView(weekdays, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val grid = GridLayout(context).apply {
            columnCount = 7
            rowCount = 6
        }
        repeat(42) { idx ->
            val cell = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                setPadding(0, dp(1), 0, 0)
                isFocusable = true
                isClickable = true
            }
            val num = TextView(context).apply {
                gravity = Gravity.CENTER
                textSize = 13f
                setTextColor(normal)
            }
            cell.addView(num, LayoutParams(dp(30), dp(30)))
            val dot = View(context)
            val dotParams = LayoutParams(dp(6), dp(6)).apply { topMargin = 0 }
            cell.addView(dot, dotParams)
            val gridParams = GridLayout.LayoutParams(
                GridLayout.spec(idx / 7), GridLayout.spec(idx % 7, 1f)
            ).apply {
                width = 0
                height = dp(44)
                setGravity(Gravity.FILL_HORIZONTAL)
            }
            grid.addView(cell, gridParams)
            cells.add(cell)
            dayNumbers.add(num)
            dots.add(dot)
        }
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val legend = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(2))
        }
        for ((color, title) in listOf(
            reached to R.string.history_goal_met,
            below to R.string.history_goal_missed,
            empty to R.string.history_calendar_no_data
        )) {
            val item = LinearLayout(context).apply { gravity = Gravity.CENTER }
            val mark = View(context).apply { background = if (color == empty) noDataRing() else shape(color, 5) }
            item.addView(mark, LayoutParams(dp(7), dp(7)))
            val label = TextView(context).apply {
                setText(title)
                setTextColor(subdued)
                textSize = 9f
                maxLines = 2
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), 0, 0, 0)
            }
            item.addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            legend.addView(item, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        addView(legend, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    fun configure(selected: Long, earliest: Long, latest: Long,
                  historical: Map<Long, Long>, liveSteps: Int, dailyGoal: Int) {
        selectedDay = DailyHistorySummary.startOfDay(selected)
        minDay = DailyHistorySummary.startOfDay(earliest)
        maxDay = DailyHistorySummary.startOfDay(latest)
        daily = historical
        todaySteps = liveSteps
        goal = dailyGoal.coerceAtLeast(1)
        month.timeInMillis = selectedDay
        month.set(Calendar.DAY_OF_MONTH, 1)
        render()
    }

    /** Avoid allocating 42 day cells after every sensor update. */
    fun updateTodaySteps(steps: Int) {
        val before = todaySteps >= goal
        todaySteps = steps
        if (before != (steps >= goal)) render()
    }

    private fun shiftMonth(offset: Int) {
        val candidate = (month.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
        val minMonth = Calendar.getInstance().apply { timeInMillis = minDay; set(Calendar.DAY_OF_MONTH, 1) }
        val maxMonth = Calendar.getInstance().apply { timeInMillis = maxDay; set(Calendar.DAY_OF_MONTH, 1) }
        if (candidate.timeInMillis in minMonth.timeInMillis..maxMonth.timeInMillis) {
            month = candidate
            render()
        }
    }

    private fun render() {
        heading.text = SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(month.time)
            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
        val minMonth = Calendar.getInstance().apply { timeInMillis = minDay; set(Calendar.DAY_OF_MONTH, 1) }
        val maxMonth = Calendar.getInstance().apply { timeInMillis = maxDay; set(Calendar.DAY_OF_MONTH, 1) }
        val canBack = month.timeInMillis > minMonth.timeInMillis
        val canNext = month.timeInMillis < maxMonth.timeInMillis
        previousMonth.isEnabled = canBack
        nextMonth.isEnabled = canNext
        previousMonth.alpha = if (canBack) 1f else 0.3f
        nextMonth.alpha = if (canNext) 1f else 0.3f
        val first = (month.clone() as Calendar)
        val before = (first.get(Calendar.DAY_OF_WEEK) - first.firstDayOfWeek + 7) % 7
        first.add(Calendar.DAY_OF_MONTH, -before)
        // A month needs four, five or six rows: don't reserve a blank sixth row.
        val visibleWeeks = (before + month.getActualMaximum(Calendar.DAY_OF_MONTH) + 6) / 7
        val fmt = SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault())
        for (index in 0 until 42) {
            val day = (first.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }
            val value = DailyHistorySummary.startOfDay(day.timeInMillis)
            val inMonth = day.get(Calendar.MONTH) == month.get(Calendar.MONTH) &&
                day.get(Calendar.YEAR) == month.get(Calendar.YEAR)
            val allowed = inMonth && value in minDay..maxDay
            val info = DailyHistorySummary.describe(value, daily, todaySteps, goal)
            val isSelected = value == selectedDay && allowed
            val number = dayNumbers[index]
            val dot = dots[index]
            val cell = cells[index]
            // GONE cells contribute no height to their GridLayout row.
            cell.visibility = if (index < visibleWeeks * 7) View.VISIBLE else View.GONE
            number.text = if (inMonth) day.get(Calendar.DAY_OF_MONTH).toString() else ""
            number.setTextColor(if (isSelected) Color.WHITE else normal)
            number.background = if (isSelected) shape(accent, 24) else null
            val dotColor = when {
                !allowed -> empty
                !info.hasData -> empty
                info.goalReached -> reached
                else -> below
            }
            dot.background = if (allowed && !info.hasData) noDataRing() else shape(dotColor, 10)
            dot.visibility = if (allowed) View.VISIBLE else View.INVISIBLE
            cell.isEnabled = allowed
            cell.alpha = if (allowed) 1f else 0.35f
            cell.contentDescription = if (allowed) {
                val status = when {
                    !info.hasData -> context.getString(R.string.history_no_data)
                    info.goalReached -> context.getString(R.string.history_goal_met)
                    else -> context.getString(R.string.history_goal_missed)
                }
                fmt.format(day.time) + ", " + status
            } else ""
            cell.setOnClickListener(if (allowed) View.OnClickListener { onDaySelected?.invoke(value) } else null)
        }
    }
}
