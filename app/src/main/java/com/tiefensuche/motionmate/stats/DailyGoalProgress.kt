/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.tiefensuche.motionmate.stats

internal data class DailyGoalState(val fillFraction: Float, val percent: Int)

/** One day's live steps versus the configured goal. No database access. */
internal object DailyGoalProgress {
    fun calculate(steps: Int, goal: Int): DailyGoalState {
        require(steps >= 0) { "Steps must be non-negative" }
        require(goal > 0) { "Goal must be positive" }

        // Keep the drawing within the shoe, but preserve percentages beyond 100%.
        val fill = (steps.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
        val rounded = ((steps.toLong() * 100L + goal.toLong() / 2L) /
            goal.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return DailyGoalState(fillFraction = fill, percent = rounded)
    }
}
