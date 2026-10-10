/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.tiefensuche.motionmate.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyGoalProgressTest {
    @Test fun emptyDay() {
        val result = DailyGoalProgress.calculate(0, 10000)
        assertEquals(0f, result.fillFraction, 0.0001f)
        assertEquals(0, result.percent)
    }

    @Test fun halfGoal() {
        val result = DailyGoalProgress.calculate(5000, 10000)
        assertEquals(0.5f, result.fillFraction, 0.0001f)
        assertEquals(50, result.percent)
    }

    @Test fun fractionalPercentageIsRounded() {
        assertEquals(33, DailyGoalProgress.calculate(3333, 10000).percent)
        assertEquals(34, DailyGoalProgress.calculate(3350, 10000).percent)
    }

    @Test fun fullGoal() {
        val result = DailyGoalProgress.calculate(10000, 10000)
        assertEquals(1f, result.fillFraction, 0.0001f)
        assertEquals(100, result.percent)
    }

    @Test fun exceededGoalCapsDrawingOnly() {
        val result = DailyGoalProgress.calculate(12500, 10000)
        assertEquals(1f, result.fillFraction, 0.0001f)
        assertEquals(125, result.percent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroGoal() { DailyGoalProgress.calculate(100, 0) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeSteps() { DailyGoalProgress.calculate(-1, 10000) }
}
