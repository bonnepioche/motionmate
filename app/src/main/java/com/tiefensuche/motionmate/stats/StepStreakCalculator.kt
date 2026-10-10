/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.motionmate.stats

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

internal data class StreakResult(
    val current: Int,
    val best: Int
)

internal object StepStreakCalculator {

    fun calculate(
        history: List<Pair<Long, Int>>,
        todaySteps: Int,
        goal: Int,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): StreakResult {
        require(goal > 0) { "Goal must be positive" }
        require(todaySteps >= 0) { "Steps cannot be negative" }

        fun calendar(timestamp: Long): Calendar =
            Calendar.getInstance(timeZone).apply {
                timeInMillis = timestamp
            }

        fun dayKey(date: Calendar): String =
            "%04d-%02d-%02d".format(
                Locale.ROOT,
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH) + 1,
                date.get(Calendar.DAY_OF_MONTH)
            )

        fun previousDay(key: String): String {
            val parts = key.split("-")
            val date = Calendar.getInstance(timeZone).apply {
                clear()
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                add(Calendar.DAY_OF_MONTH, -1)
            }
            return dayKey(date)
        }

        val today = dayKey(calendar(now))
        val yesterday = previousDay(today)

        // Sum all records for the same local calendar day, without changing stored data.
        val dailySteps = mutableMapOf<String, Long>()
        for ((timestamp, steps) in history) {
            require(steps >= 0) { "Steps cannot be negative" }
            val day = dayKey(calendar(timestamp))

            // The live counter is authoritative for today. Ignore future records.
            if (day < today) {
                dailySteps[day] = Math.addExact(dailySteps[day] ?: 0L, steps.toLong())
            }
        }
        dailySteps[today] = todaySteps.toLong()

        val completedDays = dailySteps.filterValues { it >= goal.toLong() }.keys

        var best = 0
        var run = 0
        var previous: String? = null
        for (day in completedDays.sorted()) {
            run = if (previous != null && previousDay(day) == previous) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }

        val lastEligibleDay = if (todaySteps >= goal) today else yesterday
        var current = 0
        var cursor = lastEligibleDay
        while (cursor in completedDays) {
            current++
            cursor = previousDay(cursor)
        }

        return StreakResult(current = current, best = best)
    }
}
