/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.motionmate.stats

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StepStreakCalculatorTest {

    private val tz = TimeZone.getTimeZone("Europe/Paris")

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(tz).apply {
            clear()
            set(year, month - 1, day, 12, 0)
        }.timeInMillis

    @Test
    fun currentStreakIncludesTodayWhenGoalReached() {
        val history = listOf(
            date(2026, 10, 7) to 12000,
            date(2026, 10, 8) to 11000
        )
        val result = StepStreakCalculator.calculate(history, 10500, 10000, date(2026, 10, 9), tz)
        assertEquals(3, result.current)
        assertEquals(3, result.best)
    }

    @Test
    fun currentStreakSurvivesIncompleteToday() {
        val history = listOf(
            date(2026, 10, 7) to 12000,
            date(2026, 10, 8) to 11000
        )
        val result = StepStreakCalculator.calculate(history, 5000, 10000, date(2026, 10, 9), tz)
        assertEquals(2, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun brokenStreakReturnsZero() {
        val history = listOf(
            date(2026, 10, 7) to 12000,
            date(2026, 10, 8) to 3000
        )
        val result = StepStreakCalculator.calculate(history, 5000, 10000, date(2026, 10, 9), tz)
        assertEquals(0, result.current)
        assertEquals(1, result.best)
    }

    @Test
    fun duplicateDatesAreSummed() {
        val history = listOf(
            date(2026, 10, 8) to 6000,
            date(2026, 10, 8) to 5000
        )
        val result = StepStreakCalculator.calculate(history, 12000, 10000, date(2026, 10, 9), tz)
        assertEquals(2, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun yearBoundaryIsContinuous() {
        val history = listOf(date(2025, 12, 31) to 12000)
        val result = StepStreakCalculator.calculate(history, 11000, 10000, date(2026, 1, 1), tz)
        assertEquals(2, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun missingDayBreaksStreak() {
        val history = listOf(
            date(2026, 10, 6) to 12000,
            date(2026, 10, 8) to 11000
        )
        val result = StepStreakCalculator.calculate(history, 12000, 10000, date(2026, 10, 9), tz)
        assertEquals(2, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun futureEntriesMustNotAffectStreak() {
        val history = listOf(
            date(2026, 10, 8) to 11000,
            date(2026, 10, 10) to 12000
        )
        val result = StepStreakCalculator.calculate(history, 12000, 10000, date(2026, 10, 9), tz)
        assertEquals(2, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun summerTimeChangeDoesNotBreakStreak() {
        val history = listOf(
            date(2026, 3, 28) to 12000,
            date(2026, 3, 29) to 12000
        )
        val result = StepStreakCalculator.calculate(history, 12000, 10000, date(2026, 3, 30), tz)
        assertEquals(3, result.current)
        assertEquals(3, result.best)
    }

    @Test
    fun emptyHistoryBelowGoalHasNoStreak() {
        val result = StepStreakCalculator.calculate(emptyList(), 500, 10000, date(2026, 10, 9), tz)
        assertEquals(0, result.current)
        assertEquals(0, result.best)
    }
}
