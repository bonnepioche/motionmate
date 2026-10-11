/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.CompoundButtonCompat
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.stats.ProgressIconOptions

/** Previews are drawn by the same view as the daily dashboard: no duplicate artwork. */
internal class ProgressIconPreviewAdapter(
    private val context: Context,
    private val labels: Array<CharSequence>,
    private val values: Array<CharSequence>,
    private val descriptions: Array<String>,
    selected: String
) : BaseAdapter() {
    private val selectedValue = ProgressIconOptions.normalize(selected)

    init {
        require(labels.size == 10 && values.size == 10 && descriptions.size == 10)
    }

    override fun getCount(): Int = values.size
    override fun getItem(position: Int): Any = values[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Ten items only. Re-create each short row to keep binding simple and correct.
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            minimumHeight = dp(84)
        }
        val value = values[position].toString()
        val preview = GoalProgressView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
            setIconStyle(value)
            setProgress(0.75f, 75)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        row.addView(preview)

        val descriptionBlock = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }
        row.addView(descriptionBlock, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        descriptionBlock.addView(TextView(context).apply {
            text = labels[position]
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.bronze_on_surface))
            maxLines = 2
        })
        descriptionBlock.addView(TextView(context).apply {
            text = descriptions[position]
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.bronze_on_surface_variant))
            maxLines = 3
        })
        row.addView(RadioButton(context).apply {
            isChecked = value == selectedValue
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            CompoundButtonCompat.setButtonTintList(
                this,
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.bronze_primary))
            )
        })
        row.contentDescription = "${labels[position]}, ${descriptions[position]}"
        return row
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
