/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

internal class WeeklyStepSummaryTest {
    private val tz = TimeZone.getTimeZone("Europe/Paris")

    private fun day(year: Int, month: Int, date: Int, hour: Int = 0): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, date, hour, 0)
        }.timeInMillis

    @Test fun `sums multiple records on the same local day`() {
        val result = WeeklyStepSummary.calculate(
            listOf(day(2026, 10, 5, 9) to 4000, day(2026, 10, 5, 20) to 7000,
                day(2026, 10, 6) to 12000),
            selectedDate = day(2026, 10, 7), todaySteps = 0, goal = 10000,
            now = day(2026, 10, 20), timeZone = tz, firstDayOfWeek = Calendar.MONDAY
        )
        assertEquals(11000L, result.days[0]); assertEquals(12000L, result.days[1])
        assertEquals(23000L, result.total); assertEquals(3286L, result.averagePerElapsedDay)
        assertEquals(2, result.achievedDays); assertEquals(7, result.elapsedDays)
    }

    @Test fun `today is live and average excludes future days`() {
        val result = WeeklyStepSummary.calculate(
            listOf(day(2026, 10, 5) to 12000, day(2026, 10, 6) to 4000,
                day(2026, 10, 7) to 99999), // must NOT be added to live steps
            selectedDate = day(2026, 10, 7), todaySteps = 11000, goal = 10000,
            now = day(2026, 10, 7, 15), timeZone = tz, firstDayOfWeek = Calendar.MONDAY
        )
        assertEquals(listOf(12000L, 4000L, 11000L, 0L, 0L, 0L, 0L), result.days)
        assertEquals(3, result.elapsedDays); assertEquals(2, result.todayIndex)
        assertEquals(27000L, result.total); assertEquals(9000L, result.averagePerElapsedDay)
        assertEquals(2, result.achievedDays)
    }

    @Test fun `sunday daylight saving still belongs to same week`() {
        val result = WeeklyStepSummary.calculate(
            listOf(day(2026, 3, 29, 15) to 12000),
            selectedDate = day(2026, 3, 28), todaySteps = 0, goal = 10000,
            now = day(2026, 4, 6), timeZone = tz, firstDayOfWeek = Calendar.MONDAY
        )
        assertEquals(12000L, result.days[6]); assertEquals(1, result.achievedDays)
        assertEquals(day(2026, 3, 30), result.weekEndExclusive)
    }

    @Test fun `year boundaries do not confuse current week`() {
        val result = WeeklyStepSummary.calculate(
            listOf(day(2025, 12, 29) to 14000),
            selectedDate = day(2026, 1, 1), todaySteps = 7000, goal = 10000,
            now = day(2026, 1, 1, 14), timeZone = tz, firstDayOfWeek = Calendar.MONDAY
        )
        assertEquals(4, result.elapsedDays); assertEquals(21000L, result.total)
        assertEquals(1, result.achievedDays)
    }

    @Test fun `future week has no elapsed days`() {
        val result = WeeklyStepSummary.calculate(
            emptyList(), selectedDate = day(2026, 12, 7), todaySteps = 0,
            goal = 10000, now = day(2026, 10, 11), timeZone = tz,
            firstDayOfWeek = Calendar.MONDAY
        )
        assertEquals(0, result.elapsedDays); assertEquals(0L, result.total)
    }
}
