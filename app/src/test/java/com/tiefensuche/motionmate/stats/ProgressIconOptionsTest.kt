/* SPDX-License-Identifier: GPL-3.0-only */
package com.tiefensuche.motionmate.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressIconOptionsTest {
    @Test fun defaultIsCircularGauge() {
        assertEquals("05", ProgressIconOptions.DEFAULT)
        assertEquals("05", ProgressIconOptions.normalize(null))
        assertEquals("05", ProgressIconOptions.normalize("99"))
        assertEquals("05", ProgressIconOptions.normalize("shoe"))
    }

    @Test fun allTenOptionsAreRecognized() {
        for (i in 1..10) {
            val value = "%02d".format(java.util.Locale.ROOT, i)
            assertEquals(value, ProgressIconOptions.normalize(value))
        }
        assertEquals(10, ProgressIconOptions.VALID.size)
    }
}
