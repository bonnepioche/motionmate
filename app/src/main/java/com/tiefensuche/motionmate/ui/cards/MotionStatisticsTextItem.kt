/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.motionmate.ui.cards

import android.content.Context
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.util.Util

/**
 * Further specialized [TextItem] that is used to show statistics. It is initialized with
 * data from database and updated with data from step sensor.
 */
internal class MotionStatisticsTextItem(context: Context, description: Int, var initialSteps: Int) : MotionTextItem(context, description) {
    private val monthly = description == R.string.steps_month
    private val displayContext = context

    override fun updateSteps(steps: Int) {
        val total = steps + initialSteps
        if (monthly) {
            // Only the monthly card changes order; other distance cards keep theirs.
            setContent(displayContext.getString(R.string.steps_month_card_format,
                total, Util.stepsToMeters(total)))
        } else {
            setContent(total)
        }
    }
}