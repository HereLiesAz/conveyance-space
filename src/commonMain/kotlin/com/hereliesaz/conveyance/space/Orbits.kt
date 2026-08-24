package com.hereliesaz.conveyance.space

import kotlin.math.pow

/**
 * Real orbital mechanics: Kepler's third law -- a body's orbital period scales with the 1.5 power
 * of its orbital radius, for circular orbits around a shared central mass. An outer planet isn't
 * given a slower period by eye; it's derived from how far out it sits, the same relationship that
 * makes Neptune's year 165 times Earth's rather than some author's chosen ratio.
 */
object Orbits {
    /**
     * The orbital period at [radiusPx], given one known reference orbit ([referenceRadiusPx],
     * [referencePeriodMillis]) -- Kepler's third law, `T ∝ r^1.5`.
     */
    fun periodMillisFor(radiusPx: Float, referenceRadiusPx: Float, referencePeriodMillis: Int): Int {
        if (referenceRadiusPx <= 0f) return referencePeriodMillis
        val ratio = radiusPx / referenceRadiusPx
        return (referencePeriodMillis * ratio.toDouble().pow(1.5)).toInt().coerceAtLeast(1)
    }
}
