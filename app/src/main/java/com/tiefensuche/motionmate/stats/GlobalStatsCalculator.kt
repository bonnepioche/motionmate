/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

/** Preserve the app's existing monthly, per-record average and all-time calculations.
 * The current day is held separately by MotionService and must be added exactly once.
 */
internal data class GlobalStatsResult(
    val monthSteps: Long,
    val averageSteps: Long,
    val overallSteps: Long
)

internal object GlobalStatsCalculator {
    fun calculate(
        monthHistorySteps: Int,
        averageHistorySteps: Int,
        overallHistorySteps: Int,
        todaySteps: Int,
        includeTodayInMonth: Boolean
    ): GlobalStatsResult {
        require(monthHistorySteps >= 0 && averageHistorySteps >= 0 && overallHistorySteps >= 0)
        require(todaySteps >= 0)
        return GlobalStatsResult(
            monthSteps = monthHistorySteps.toLong() + if (includeTodayInMonth) todaySteps.toLong() else 0L,
            averageSteps = averageHistorySteps.toLong(),
            overallSteps = overallHistorySteps.toLong() + todaySteps.toLong()
        )
    }
}
