/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.tiefensuche.motionmate.ui


import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.tiefensuche.motionmate.R
import com.tiefensuche.motionmate.ui.cards.TextItem
import com.tiefensuche.motionmate.ui.cards.StreakDashboardItem
import com.tiefensuche.motionmate.ui.cards.RollingStepAveragesItem
import com.tiefensuche.motionmate.ui.cards.GlobalStatsItem
import java.util.*

/**
 * An adapter for the [RecyclerView] that contains [TextItem].
 * It contains dynamically changing items that display statistics and activities.
 *
 *
 * Created by tiefensuche on 19.10.16.
 */
internal class TextItemAdapter : RecyclerView.Adapter<TextItemAdapter.ViewHolder>() {
    private val mDataset = ArrayList<TextItem>()
    private var dashboardCardStyle = "standard"

    /** Rebind the same models with a different visual treatment; no data is recalculated. */
    internal fun setDashboardCardStyle(style: String) {
        val normalized = if (style == "gradient") "gradient" else "standard"
        if (dashboardCardStyle != normalized) {
            dashboardCardStyle = normalized
            notifyDataSetChanged()
        }
    }

    internal fun addTop(item: TextItem) {
        add(item, 0)
    }

    internal fun add(item: TextItem, position: Int = mDataset.size) {
        mDataset.add(position, item)
        notifyItemInserted(position)
    }

    internal fun remove(index: Int) {
        mDataset.removeAt(index)
        notifyItemRemoved(index)
    }

    internal operator fun get(index: Int): TextItem {
        return mDataset[index]
    }

    override fun getItemViewType(position: Int): Int = when (val item = mDataset[position]) {
        is StreakDashboardItem -> when (item.displayStyle) {
            "compact_b" -> 3
            "compact_c" -> 4
            else -> 1
        }
        is RollingStepAveragesItem -> 2
        is GlobalStatsItem -> 5
        else -> 0
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val layout = when (viewType) {
            1 -> R.layout.cardview_streak
            2 -> R.layout.cardview_averages
            3 -> R.layout.cardview_streak_compact_b
            4 -> R.layout.cardview_streak_compact_c
            5 -> R.layout.cardview_global_stats
            else -> R.layout.cardview_text
        }
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mDataset[position]
        val compact = item is StreakDashboardItem && item.displayStyle != "current"
        holder.mTextViewDescription.text = item.description
        holder.mTextViewContent.text = item.content
        bindStatsTiles(holder, item)
        if (item is GlobalStatsItem || item is RollingStepAveragesItem) {
            DashboardGradientStyler.apply(holder.itemView, item is GlobalStatsItem,
                dashboardCardStyle == "gradient")
        } else if (item is StreakDashboardItem) {
            DashboardGradientStyler.applyCard(holder.itemView, R.id.streakGradientSurface,
                dashboardCardStyle == "gradient")
        } else {
            // The generic statistics/activity card uses the same appearance preference.
            DashboardGradientStyler.applyCard(holder.itemView, R.id.genericGradientSurface,
                dashboardCardStyle == "gradient")
        }
        if (item is StreakDashboardItem) {
            holder.mBestStreak?.text = if (compact) item.compactBestText else item.bestText
            holder.mStreakGoal?.text = item.goalText
            holder.mBestPeriod?.text = item.periodText
            fun syncDetails() {
                holder.mBestPeriod?.visibility = if (!compact ||
                    (item.detailsExpanded && item.periodText.isNotEmpty())) View.VISIBLE else View.GONE
                holder.mStreakGoal?.visibility = if (!compact || item.detailsExpanded) View.VISIBLE else View.GONE
            }
            syncDetails()
            if (compact) {
                holder.itemView.isFocusable = true
                holder.itemView.setOnClickListener {
                    item.detailsExpanded = !item.detailsExpanded
                    syncDetails()
                }
            } else {
                holder.itemView.setOnClickListener(null)
                holder.itemView.isFocusable = false
            }
        } else {
            holder.itemView.setOnClickListener(null)
        }
        if (item.buttonClickListener != null && holder.mImageButton != null) {
            holder.mImageButton.setOnClickListener(item.buttonClickListener)
            holder.mImageButton.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            holder.mImageButton?.visibility = View.GONE
        }
        item.setUpdateListener(object : TextItem.UpdateListener {
            override fun update(text: String?) {
                holder.mTextViewContent.text = text
                bindStatsTiles(holder, item)
                if (item is StreakDashboardItem) {
                    holder.mBestStreak?.text = if (compact) item.compactBestText else item.bestText
                    holder.mStreakGoal?.text = item.goalText
                    holder.mBestPeriod?.text = item.periodText
                }
            }

            override fun updateIcon(id: Int) {
                holder.mImageButton?.setImageResource(id)
            }
        })
    }

    private fun bindStatsTiles(holder: ViewHolder, item: TextItem) {
        if (item is GlobalStatsItem) {
            holder.itemView.findViewById<TextView>(R.id.globalMonthValue)?.text = item.monthValue()
            holder.itemView.findViewById<TextView>(R.id.globalAverageValue)?.text = item.averageValue()
            holder.itemView.findViewById<TextView>(R.id.globalOverallValue)?.text = item.overallValue()
        }
        if (item is RollingStepAveragesItem) {
            val views = listOf(
                7 to R.id.rollingAvg7, 30 to R.id.rollingAvg30, 90 to R.id.rollingAvg90,
                180 to R.id.rollingAvg180, 365 to R.id.rollingAvg365
            )
            for ((days, id) in views) {
                holder.itemView.findViewById<TextView>(id)?.text = item.formattedValue(days)
            }
        }
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }

    internal inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        internal val mTextViewDescription: TextView = v.findViewById(R.id.textViewDescription)
        internal val mTextViewContent: TextView = v.findViewById(R.id.textViewContent)
        internal val mImageButton: ImageButton? = v.findViewById(R.id.imageButton)
        internal val mBestStreak: TextView? = v.findViewById(R.id.textViewBestStreak)
        internal val mStreakGoal: TextView? = v.findViewById(R.id.textViewStreakGoal)
        internal val mBestPeriod: TextView? = v.findViewById(R.id.textViewBestPeriod)
    }

}