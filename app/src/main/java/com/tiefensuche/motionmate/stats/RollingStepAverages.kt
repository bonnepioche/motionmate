/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

import java.util.Calendar
import java.util.TimeZone

/** Averages over the last N *complete* local calendar days; missing days are zero. */
internal object RollingStepAverages {
    val windows = listOf(7, 30, 90, 180, 365)

    fun calculate(
        history: List<Pair<Long, Int>>,
        now: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Map<Int, Long> {
        val today = Calendar.getInstance(timeZone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = today.timeInMillis
        val daily = mutableMapOf<Long, Long>()
        for ((timestamp, steps) in history) {
            require(steps >= 0) { "Steps cannot be negative" }
            val day = Calendar.getInstance(timeZone).apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            if (day < todayStart) {
                daily[day] = Math.addExact(daily[day] ?: 0L, steps.toLong())
            }
        }

        val result = linkedMapOf<Int, Long>()
        val cursor = (today.clone() as Calendar)
        var sum = 0L
        for (daysAgo in 1..windows.last()) {
            cursor.add(Calendar.DAY_OF_MONTH, -1)
            sum = Math.addExact(sum, daily[cursor.timeInMillis] ?: 0L)
            if (daysAgo in windows) {
                // Rounded to the nearest whole step, with half values rounded up.
                result[daysAgo] = (sum + daysAgo / 2L) / daysAgo
            }
        }
        return result
    }
}
