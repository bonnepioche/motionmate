/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class RollingStepAveragesTest {
    private val tz = TimeZone.getTimeZone("Europe/Paris")

    private fun date(y: Int, m: Int, d: Int, hour: Int = 12): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(y, m - 1, d, hour, 0)
        }.timeInMillis

    @Test fun sumsSameDayAndCountsMissingDaysAsZero() {
        val history = listOf(
            date(2026, 10, 9) to 6000,
            date(2026, 10, 9, 18) to 5000,
            date(2026, 10, 8) to 9000
        )
        val r = RollingStepAverages.calculate(history, date(2026, 10, 10), tz)
        assertEquals(2857L, r[7])
        assertEquals(667L, r[30])
    }

    @Test fun excludesTodayAndFuture() {
        val history = listOf(
            date(2026, 10, 9) to 7000,
            date(2026, 10, 10) to 25000,
            date(2026, 10, 11) to 10000
        )
        assertEquals(1000L, RollingStepAverages.calculate(history, date(2026, 10, 10), tz)[7])
    }

    @Test fun handlesDstAndLeapDay() {
        val history = listOf(date(2024, 2, 29) to 14000)
        assertEquals(2000L, RollingStepAverages.calculate(history, date(2024, 3, 1), tz)[7])
        val spring = listOf(date(2026, 3, 28) to 7000, date(2026, 3, 29) to 7000)
        assertEquals(2000L, RollingStepAverages.calculate(spring, date(2026, 3, 30), tz)[7])
    }

    @Test fun emptyHistoryIsZero() {
        val r = RollingStepAverages.calculate(emptyList(), date(2026, 10, 10), tz)
        assertEquals(listOf(7, 30, 90, 180, 365), r.keys.toList())
        assertEquals(0L, r[365])
    }
}
