/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StepStreakDatesTest {
    private val tz = TimeZone.getTimeZone("Europe/Paris")
    private fun date(y: Int, m: Int, d: Int): Long =
        Calendar.getInstance(tz).apply { clear(); set(y, m - 1, d, 12, 0) }.timeInMillis

    private fun formatDay(timestamp: Long?): String? {
        if (timestamp == null) return null
        val cal = Calendar.getInstance(tz).apply { timeInMillis = timestamp }
        return "%04d-%02d-%02d".format(
            java.util.Locale.ROOT,
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    @Test fun findsRecordDatesCrossingYear() {
        val result = StepStreakCalculator.calculate(
            listOf(date(2025, 12, 30) to 11000, date(2025, 12, 31) to 12000),
            14000, 10000, date(2026, 1, 1), tz
        )
        assertEquals(3, result.best)
        assertEquals("2025-12-30", formatDay(result.bestStart))
        assertEquals("2026-01-01", formatDay(result.bestEnd))
    }

    @Test fun latestRunWinsOnEqualRecord() {
        val result = StepStreakCalculator.calculate(
            listOf(
                date(2026, 1, 1) to 11000, date(2026, 1, 2) to 11000,
                date(2026, 1, 4) to 11000, date(2026, 1, 5) to 11000
            ), 0, 10000, date(2026, 1, 6), tz
        )
        assertEquals("2026-01-04", formatDay(result.bestStart))
        assertEquals("2026-01-05", formatDay(result.bestEnd))
    }

    @Test fun noRecordDatesWithoutCompletedDays() {
        val result = StepStreakCalculator.calculate(emptyList(), 0, 10000, date(2026, 1, 6), tz)
        assertEquals(0, result.best)
        assertNull(result.bestStart)
        assertNull(result.bestEnd)
    }
}
