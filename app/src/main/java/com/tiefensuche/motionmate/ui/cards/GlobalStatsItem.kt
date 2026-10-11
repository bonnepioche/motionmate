/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui.cards

import android.content.Context
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.stats.GlobalStatsCalculator
import com.tiefensuche.motionmate.stats.GlobalStatsResult
import com.tiefensuche.motionmate.util.Util
import java.text.NumberFormat

/** A single card grouping the three existing statistics; it never writes step data. */
internal class GlobalStatsItem(private val appContext: Context) :
    TextItem(appContext, R.string.global_stats_title) {

    private var monthHistory = 0
    private var averageHistory = 0
    private var overallHistory = 0
    private var today = 0
    private var includeTodayInMonth = true

    private var snapshot = GlobalStatsResult(0L, 0L, 0L)

    fun updateHistory(month: Int, average: Int, overall: Int, currentMonth: Boolean) {
        monthHistory = month
        averageHistory = average
        overallHistory = overall
        includeTodayInMonth = currentMonth
        refresh()
    }

    fun updateToday(steps: Int) {
        today = steps.coerceAtLeast(0)
        refresh()
    }

    fun monthValue(): String = format(snapshot.monthSteps)
    fun averageValue(): String = format(snapshot.averageSteps)
    fun overallValue(): String = format(snapshot.overallSteps)

    private fun format(steps: Long): String {
        val grouped = NumberFormat.getIntegerInstance().format(steps)
        // Keep the existing configured stride length and French decimal formatting.
        val kilometers = steps.toDouble() * Util.stepWidth / 100000.0
        return appContext.getString(R.string.global_stats_value, grouped, kilometers)
    }

    private fun refresh() {
        snapshot = GlobalStatsCalculator.calculate(
            monthHistory, averageHistory, overallHistory, today, includeTodayInMonth
        )
        // Also provide a readable fallback/accessibility string for TextItemAdapter.
        setContent(appContext.getString(R.string.global_stats_accessibility,
            monthValue(), averageValue(), overallValue()))
    }
}
