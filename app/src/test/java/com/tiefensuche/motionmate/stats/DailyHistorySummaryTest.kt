/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

internal class DailyHistorySummaryTest {
    private val paris = TimeZone.getTimeZone("Europe/Paris")
    private fun day(y: Int, m: Int, d: Int, hour: Int = 0): Long =
        Calendar.getInstance(paris).apply {
            clear()
            set(y, m - 1, d, hour, 0)
        }.timeInMillis

    @Test fun addsMultipleRecordsForSameDay() {
        val now = day(2026, 10, 11, 12)
        val days = DailyHistorySummary.aggregate(
            listOf(day(2026, 10, 9, 2) to 4000,
                   day(2026, 10, 9, 17) to 7000), now, paris)
        val info = DailyHistorySummary.describe(day(2026, 10, 9), days, 6780, 10000, now, paris)
        assertEquals(11000L, info.steps)
        assertTrue(info.goalReached)
        assertEquals(110, info.percent)
    }

    @Test fun todayAlwaysUsesLiveSensorValue() {
        val now = day(2026, 10, 11, 14)
        val days = DailyHistorySummary.aggregate(listOf(day(2026, 10, 11) to 16000), now, paris)
        assertTrue(days.isEmpty())
        val info = DailyHistorySummary.describe(now, days, 6780, 10000, now, paris)
        assertEquals(6780L, info.steps)
        assertTrue(info.hasData)
        assertEquals(68, info.percent) // Rounded to the nearest whole percent.
    }

    @Test fun missingAndZeroAreDifferent() {
        val now = day(2026, 10, 11, 12)
        val days = DailyHistorySummary.aggregate(listOf(day(2026, 10, 7) to 0), now, paris)
        assertTrue(DailyHistorySummary.describe(day(2026, 10, 7), days, 0, 10000, now, paris).hasData)
        assertFalse(DailyHistorySummary.describe(day(2026, 10, 8), days, 0, 10000, now, paris).hasData)
        assertTrue(DailyHistorySummary.describe(day(2026, 10, 12), days, 0, 10000, now, paris).isFuture)
    }

    @Test fun localDayNotFixedTwentyFourHours() {
        val before = day(2026, 10, 25, 1)
        val after = day(2026, 10, 25, 22)
        val tomorrow = day(2026, 10, 26, 12)
        val days = DailyHistorySummary.aggregate(listOf(before to 1200, after to 4300), tomorrow, paris)
        assertEquals(5500L, days[day(2026, 10, 25)])
    }

    @Test fun dayNormalizationHandlesYearBoundary() {
        val now = day(2027, 1, 2)
        val days = DailyHistorySummary.aggregate(listOf(day(2026, 12, 31, 20) to 10001), now, paris)
        assertTrue(DailyHistorySummary.describe(day(2026, 12, 31), days, 0, 10000, now, paris).goalReached)
    }
}
