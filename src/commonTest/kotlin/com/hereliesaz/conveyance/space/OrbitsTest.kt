package com.hereliesaz.conveyance.space

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrbitsTest {

    @Test
    fun `a body at the reference radius gets exactly the reference period`() {
        assertEquals(1000, Orbits.periodMillisFor(radiusPx = 50f, referenceRadiusPx = 50f, referencePeriodMillis = 1000))
    }

    /** Kepler's third law: T proportional to r^1.5 -- a body at 4x the reference radius orbits in 4^1.5 = 8x the period. */
    @Test
    fun `a body at 4x the reference radius takes 8x the reference period`() {
        val period = Orbits.periodMillisFor(radiusPx = 200f, referenceRadiusPx = 50f, referencePeriodMillis = 1000)
        assertEquals(8000, period)
    }

    /** Neptune's real year is about 165x Earth's, at roughly 30x Earth's orbital radius (30^1.5 ~= 164). */
    @Test
    fun `a body 30x further out takes roughly 165x as long, matching real Kepler proportions`() {
        val period = Orbits.periodMillisFor(radiusPx = 30f, referenceRadiusPx = 1f, referencePeriodMillis = 365)
        assertTrue(period in 59000..61000, "expected roughly 365*164, was $period")
    }

    @Test
    fun `an outer orbit always takes longer than an inner one, all else equal`() {
        val inner = Orbits.periodMillisFor(radiusPx = 40f, referenceRadiusPx = 40f, referencePeriodMillis = 500)
        val outer = Orbits.periodMillisFor(radiusPx = 120f, referenceRadiusPx = 40f, referencePeriodMillis = 500)
        assertTrue(outer > inner, "outer=$outer should exceed inner=$inner")
    }

    @Test
    fun `a non-positive reference radius falls back to the reference period rather than dividing by zero`() {
        assertEquals(1000, Orbits.periodMillisFor(radiusPx = 50f, referenceRadiusPx = 0f, referencePeriodMillis = 1000))
        assertEquals(1000, Orbits.periodMillisFor(radiusPx = 50f, referenceRadiusPx = -10f, referencePeriodMillis = 1000))
    }

    @Test
    fun `the result is never less than 1 millisecond`() {
        val period = Orbits.periodMillisFor(radiusPx = 0.001f, referenceRadiusPx = 1000f, referencePeriodMillis = 1)
        assertTrue(period >= 1)
    }
}
