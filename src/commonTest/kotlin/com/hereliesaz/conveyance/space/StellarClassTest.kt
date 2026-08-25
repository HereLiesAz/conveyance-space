package com.hereliesaz.conveyance.space

import kotlin.test.Test
import kotlin.test.assertEquals

class StellarClassTest {

    @Test
    fun `SpectralClass of resolves every real OBAFGKM letter, case-insensitively`() {
        assertEquals(SpectralClass.O, SpectralClass.of("O"))
        assertEquals(SpectralClass.B, SpectralClass.of("b"))
        assertEquals(SpectralClass.A, SpectralClass.of("A"))
        assertEquals(SpectralClass.F, SpectralClass.of("f"))
        assertEquals(SpectralClass.G, SpectralClass.of("G"))
        assertEquals(SpectralClass.K, SpectralClass.of("k"))
        assertEquals(SpectralClass.M, SpectralClass.of("M"))
    }

    @Test
    fun `SpectralClass of falls back to G for an unrecognized hue`() {
        assertEquals(SpectralClass.G, SpectralClass.of(""))
        assertEquals(SpectralClass.G, SpectralClass.of("nonsense"))
        assertEquals(SpectralClass.G, SpectralClass.of("Z"))
    }

    /** Hotter classes should read bluer, cooler classes redder -- real black-body color, not an arbitrary palette. */
    @Test
    fun `the seven spectral colors are genuinely distinct`() {
        assertEquals(7, SpectralClass.entries.map { it.color }.toSet().size)
    }

    @Test
    fun `StarSize of maps rank primary to Giant and tertiary to Dwarf`() {
        assertEquals(StarSize.Giant, StarSize.of("primary"))
        assertEquals(StarSize.Dwarf, StarSize.of("tertiary"))
    }

    @Test
    fun `StarSize of falls back to MainSequence for any other rank`() {
        assertEquals(StarSize.MainSequence, StarSize.of("secondary"))
        assertEquals(StarSize.MainSequence, StarSize.of(""))
        assertEquals(StarSize.MainSequence, StarSize.of("nonsense"))
    }

    @Test
    fun `Giant is the largest size and Dwarf the smallest`() {
        val sizes = StarSize.entries.sortedBy { it.diameter.value }
        assertEquals(StarSize.Dwarf, sizes.first())
        assertEquals(StarSize.Giant, sizes.last())
    }
}
