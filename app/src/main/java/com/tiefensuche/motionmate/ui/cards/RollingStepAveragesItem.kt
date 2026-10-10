/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui.cards

import android.content.Context
import com.tiefensuche.motionmate.R
import java.text.NumberFormat

/** Display averages over completed local calendar days. */
internal class RollingStepAveragesItem(private val context: Context) :
    TextItem(context, R.string.rolling_averages_title) {

    fun updateStats(averages: Map<Int, Long>) {
        val numberFormat = NumberFormat.getIntegerInstance()
        setContent(averages.entries.joinToString("\n") { (days, steps) ->
            context.getString(R.string.rolling_average_row, days, numberFormat.format(steps))
        })
    }
}
