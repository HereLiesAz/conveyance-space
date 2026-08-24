package com.hereliesaz.conveyance.space

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A star's two independent axes, the same two the real Hertzsprung-Russell diagram plots: color
 * (temperature) on one axis, size (luminosity class) on the other. [ComposableRequest.hue] picks
 * a [SpectralClass] (the OBAFGKM sequence -- hotter is bluer, cooler is redder, real black-body
 * color, not an invented palette); [ComposableRequest.rank] separately picks a size class, so a
 * hot blue star can be a small dwarf or a giant just as a cool red star can, matching how real
 * stars actually combine the two.
 */
enum class SpectralClass(val color: Color) {
    O(Color(0xFF9BB0FF)),
    B(Color(0xFFAABFFF)),
    A(Color(0xFFCAD7FF)),
    F(Color(0xFFF8F7FF)),
    G(Color(0xFFFFF4EA)),
    K(Color(0xFFFFD2A1)),
    M(Color(0xFFFFB56C)),
    ;

    companion object {
        /** Looks up a class by the composable manifest's `hue` string ("O".."M"); an unrecognized value falls back to [G] (Sun-like). */
        fun of(hue: String): SpectralClass = entries.firstOrNull { it.name == hue.uppercase() } ?: G
    }
}

/**
 * The luminosity axis: [dwarf] (a small, low-mass star), [mainSequence] (the long, stable
 * hydrogen-burning phase most stars, including the Sun, spend most of their life in), and
 * [giant] -- a star that has exhausted its core hydrogen and swollen dramatically, the real
 * mechanism [ComposableRequest.rank]'s `"primary"` maps to. Conveyance's own `Product.keystones`
 * allows only one to three -- the same real scarcity a red giant phase has: not every star
 * becomes one, and a surface with a giant on it should have at most a few.
 */
enum class StarSize(val diameter: Dp) {
    Dwarf(28.dp),
    MainSequence(48.dp),
    Giant(84.dp),
    ;

    companion object {
        /** Looks up a size by the composable manifest's `hue`-adjacent rank string. */
        fun of(rank: String): StarSize = when (rank) {
            "primary" -> Giant
            "tertiary" -> Dwarf
            else -> MainSequence
        }
    }
}
