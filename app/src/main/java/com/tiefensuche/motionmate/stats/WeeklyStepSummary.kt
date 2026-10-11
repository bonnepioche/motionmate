/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

import java.util.Calendar
import java.util.TimeZone

/** Seven local calendar days; missing past days count as zero, future days are excluded. */
internal data class WeeklyStepResult(
    val weekStart: Long,
    val weekEndExclusive: Long,
    val days: List<Long>,
    val total: Long,
    val averagePerElapsedDay: Long,
    val achievedDays: Int,
    val elapsedDays: Int,
    val todayIndex: Int,
    val goal: Int
)

internal object WeeklyStepSummary {
    fun calculate(
        history: List<Pair<Long, Int>>,
        selectedDate: Long,
        todaySteps: Int,
        goal: Int,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        firstDayOfWeek: Int = Calendar.getInstance(timeZone).firstDayOfWeek
    ): WeeklyStepResult {
        require(goal > 0) { "Goal must be positive" }
        require(todaySteps >= 0) { "Today's steps cannot be negative" }
        require(firstDayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY)

        fun startOfDay(timestamp: Long): Calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val week = startOfDay(selectedDate)
        val offset = (week.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
        week.add(Calendar.DAY_OF_MONTH, -offset)
        val start = week.timeInMillis

        // Calendar.add, rather than +86_400_000 ms, handles daylight saving changes.
        val boundaries = LongArray(8)
        for (i in 0..7) {
            boundaries[i] = week.timeInMillis
            week.add(Calendar.DAY_OF_MONTH, 1)
        }

        val todayStart = startOfDay(now).timeInMillis
        val todayIndex = (0..6).indexOfFirst { todayStart == boundaries[it] }
        val elapsedDays = when {
            todayIndex >= 0 -> todayIndex + 1
            boundaries[7] <= todayStart -> 7
            else -> 0
        }
        val days = LongArray(7)
        history.forEach { (timestamp, steps) ->
            require(steps >= 0) { "Historical steps cannot be negative" }
            if (timestamp >= start && timestamp < boundaries[7]) {
                val index = (0..6).indexOfFirst {
                    timestamp >= boundaries[it] && timestamp < boundaries[it + 1]
                }
                if (index >= 0 && index != todayIndex && index < elapsedDays) {
                    days[index] = Math.addExact(days[index], steps.toLong())
                }
            }
        }
        // Live counter is authoritative for the active day; don't double count a CSV row.
        if (todayIndex >= 0) days[todayIndex] = todaySteps.toLong()

        val total = days.take(elapsedDays).fold(0L) { sum, steps -> Math.addExact(sum, steps) }
        val average = if (elapsedDays > 0) (total + elapsedDays / 2) / elapsedDays else 0L
        val achieved = days.take(elapsedDays).count { it >= goal.toLong() }
        return WeeklyStepResult(
            weekStart = start,
            weekEndExclusive = boundaries[7],
            days = days.toList(),
            total = total,
            averagePerElapsedDay = average,
            achievedDays = achieved,
            elapsedDays = elapsedDays,
            todayIndex = todayIndex,
            goal = goal
        )
    }
}
