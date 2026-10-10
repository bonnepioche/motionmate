/*
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.tiefensuche.motionmate.ui.cards

import android.content.Context
import com.tiefensuche.motionmate.R
import java.text.NumberFormat
import java.text.DateFormat
import java.util.Date

/** One bronze card showing current streak, best streak and daily goal. */
internal class StreakDashboardItem(private val context: Context) :
    TextItem(context, R.string.current_streak) {

    var bestText: String = ""
        private set
    var goalText: String = ""
        private set
    var periodText: String = ""
        private set

    fun updateStats(current: Int, best: Int, goal: Int, bestStart: Long?, bestEnd: Long?) {
        val format = DateFormat.getDateInstance(DateFormat.MEDIUM)
        periodText = if (bestStart != null && bestEnd != null) {
            context.getString(R.string.streak_record_period,
                format.format(Date(bestStart)), format.format(Date(bestEnd)))
        } else {
            ""
        }
        bestText = context.getString(R.string.streak_best_summary,
            context.getString(R.string.streak_days, best))
        goalText = context.getString(R.string.streak_goal_summary,
            NumberFormat.getIntegerInstance().format(goal))
        // Triggers the existing adapter update listener, including best and goal labels.
        setContent(context.getString(R.string.streak_days, current))
    }
}
