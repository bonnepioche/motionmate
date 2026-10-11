/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui.cards

import android.content.Context
import com.tiefensuche.motionmate.R
import java.text.NumberFormat

/** Display averages over completed local calendar days. */
internal class RollingStepAveragesItem(private val context: Context) :
    TextItem(context, R.string.rolling_averages_title) {

    private var currentAverages: Map<Int, Long> = emptyMap()

    fun formattedValue(days: Int): String = currentAverages[days]?.let {
        NumberFormat.getIntegerInstance().format(it)
    } ?: "—"

    fun updateStats(averages: Map<Int, Long>) {
        currentAverages = averages.toMap()
        val numberFormat = NumberFormat.getIntegerInstance()
        setContent(averages.entries.joinToString("\n") { (days, steps) ->
            context.getString(R.string.rolling_average_row, days, numberFormat.format(steps))
        })
    }
}
