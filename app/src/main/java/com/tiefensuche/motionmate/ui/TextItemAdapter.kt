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
        else -> 0
    }

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ViewHolder {
        val layout = when (viewType) {
            1 -> R.layout.cardview_streak
            2 -> R.layout.cardview_averages
            3 -> R.layout.cardview_streak_compact_b
            4 -> R.layout.cardview_streak_compact_c
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