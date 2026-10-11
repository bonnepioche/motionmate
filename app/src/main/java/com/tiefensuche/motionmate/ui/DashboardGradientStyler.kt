/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.ui

import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.tiefensuche.motionmate.R

/** Purely visual styling for every dashboard card (including statistics).
 *  No database access or changes to step-counting logic.
 */
internal object DashboardGradientStyler {
    private val globalSurfaces = intArrayOf(
        R.id.globalStatsGradientSurface,
        R.id.globalMonthGradientSurface,
        R.id.globalAverageGradientSurface,
        R.id.globalOverallGradientSurface
    )

    private val averageSurfaces = intArrayOf(
        R.id.rollingGradientSurface,
        R.id.rolling7GradientSurface,
        R.id.rolling30GradientSurface,
        R.id.rolling90GradientSurface,
        R.id.rolling180GradientSurface,
        R.id.rolling365GradientSurface
    )

    /** Non-RecyclerView cards at the top of the dashboard. */
    private val dashboardSurfaces = intArrayOf(
        R.id.todayGradientSurface,
        R.id.weekGradientSurface,
        R.id.historyGradientSurface
    )

    /** Refresh the three static cards when returning from Settings. */
    fun applyDashboard(root: View, gradientEnabled: Boolean) {
        dashboardSurfaces.forEach { id ->
            applyCard(root, id, gradientEnabled)
        }
    }

    /** Refresh a RecyclerView card, including any of the three streak layouts. */
    fun applyCard(root: View, surfaceId: Int, gradientEnabled: Boolean) {
        val surface = root.findViewById<View>(surfaceId) ?: return
        // A null background restores each card's original MaterialCardView surface.
        ViewCompat.setBackground(surface,
            if (gradientEnabled) createGradient(surface, outer = true) else null)
    }

    fun apply(itemView: View, isGlobal: Boolean, gradientEnabled: Boolean) {
        val surfaces = if (isGlobal) globalSurfaces else averageSurfaces
        surfaces.forEachIndexed { index, id ->
            val view = itemView.findViewById<View>(id) ?: return@forEachIndexed
            // Standard restores the MaterialCardView colors; gradient changes visuals only.
            val drawable = if (gradientEnabled) createGradient(view, outer = index == 0) else null
            ViewCompat.setBackground(view, drawable)
        }
    }

    private fun createGradient(view: View, outer: Boolean): GradientDrawable {
        val shades = if (outer) intArrayOf(
            R.color.dashboard_gradient_outer_start,
            R.color.dashboard_gradient_outer_mid,
            R.color.dashboard_gradient_outer_end
        ) else intArrayOf(
            R.color.dashboard_gradient_tile_start,
            R.color.dashboard_gradient_tile_mid,
            R.color.dashboard_gradient_tile_end
        )
        val colors = shades.map { ContextCompat.getColor(view.context, it) }.toIntArray()
        val radius = (if (outer) 16f else 12f) * view.resources.displayMetrics.density
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = radius
        }
    }
}
