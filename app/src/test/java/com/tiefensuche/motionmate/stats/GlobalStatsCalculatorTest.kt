/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalStatsCalculatorTest {
    @Test fun currentMonthAddsLiveStepsOnce() {
        val result = GlobalStatsCalculator.calculate(1000, 420, 9000, 780, true)
        assertEquals(1780L, result.monthSteps)
        assertEquals(420L, result.averageSteps)
        assertEquals(9780L, result.overallSteps)
    }

    @Test fun historicalMonthDoesNotIncludeToday() {
        val result = GlobalStatsCalculator.calculate(1000, 420, 9000, 780, false)
        assertEquals(1000L, result.monthSteps)
        assertEquals(9780L, result.overallSteps)
    }

    @Test fun emptyDataAndLargeSums() {
        val empty = GlobalStatsCalculator.calculate(0, 0, 0, 0, true)
        assertEquals(0L, empty.overallSteps)
        val large = GlobalStatsCalculator.calculate(Int.MAX_VALUE, 10000, Int.MAX_VALUE, 50000, true)
        assertEquals(Int.MAX_VALUE.toLong() + 50000L, large.overallSteps)
        assertEquals(Int.MAX_VALUE.toLong() + 50000L, large.monthSteps)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeHistoricalValues() {
        GlobalStatsCalculator.calculate(-1, 0, 0, 0, true)
    }
}
