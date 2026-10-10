/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

/** Preference values, deliberately stable across app upgrades. */
object ProgressIconOptions {
    const val DEFAULT = "05"
    val VALID: Set<String> = (1..10).map { "%02d".format(java.util.Locale.ROOT, it) }.toSet()

    fun normalize(value: String?): String = if (value in VALID) value!! else DEFAULT
}
