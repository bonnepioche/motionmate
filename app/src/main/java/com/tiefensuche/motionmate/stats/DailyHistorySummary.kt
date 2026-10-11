/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

import java.util.Calendar
import java.util.TimeZone

/** Day-level data for History. The live counter is authoritative for today. */
internal data class HistoryDayInfo(
    val dayStart: Long,
    val steps: Long,
    val hasData: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean,
    val goalReached: Boolean,
    val percent: Int
)

internal object DailyHistorySummary {
    fun startOfDay(timestamp: Long, timeZone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Sum multiple records per local date, without altering historical entries. */
    fun aggregate(
        history: List<Pair<Long, Int>>,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Map<Long, Long> {
        val today = startOfDay(now, timeZone)
        val days = mutableMapOf<Long, Long>()
        for ((time, steps) in history) {
            require(steps >= 0) { "Historical steps cannot be negative" }
            val day = startOfDay(time, timeZone)
            // Old CSV records for today are ignored: the sensor's live count wins.
            if (day < today) {
                days[day] = Math.addExact(days[day] ?: 0L, steps.toLong())
            }
        }
        return days
    }

    fun describe(
        selected: Long,
        daily: Map<Long, Long>,
        todaySteps: Int,
        goal: Int,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): HistoryDayInfo {
        require(todaySteps >= 0) { "Today's steps cannot be negative" }
        require(goal > 0) { "Goal must be positive" }
        val day = startOfDay(selected, timeZone)
        val today = startOfDay(now, timeZone)
        val future = day > today
        val current = day == today
        val hasData = !future && (current || daily.containsKey(day))
        val steps = when {
            future -> 0L
            current -> todaySteps.toLong()
            else -> daily[day] ?: 0L
        }
        val percent = (steps.toDouble() * 100.0 / goal + 0.5).toLong()
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return HistoryDayInfo(day, steps, hasData, current, future,
            hasData && steps >= goal.toLong(), percent)
    }
}
