package com.hereliesaz.conveyance.space

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hereliesaz.conveyance.compose.Offer
import com.hereliesaz.conveyance.compose.tell
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BINARY_ORBIT_MILLIS = 5000
private const val BINARY_SEPARATION_DP = 90f

/**
 * A binary star system: [primary] and [companion] orbit their shared center of mass (the
 * barycenter), 180° out of phase -- real binary-star mechanics, not one star with a smaller
 * companion parked beside it. Each star's own orbital radius around the barycenter is inversely
 * proportional to its own [StarSize] diameter (a stand-in for mass), the two radii always summing
 * to [BINARY_SEPARATION_DP]: the more massive (larger) star sits closer to the barycenter, the
 * lighter one swings wider -- the same reason Sirius A, the heavier of that real pair, traces the
 * tighter orbit.
 *
 * This is **not** a [Templates.registry] entry, for the same reason [LiquidField]
 * (`conveyance-liquid`) isn't: it needs *two* independent [ComposableRequest]s, each with its own
 * `act`, a shape a single composable manifest element (exactly one `act` --
 * azphalt `spec/composable.md`) can't express. A host wires this up directly.
 */
@Composable
fun BinaryStarSystem(primary: ComposableRequest, companion: ComposableRequest) {
    val primarySize = StarSize.of(primary.rank)
    val companionSize = StarSize.of(companion.rank)
    val primaryClass = SpectralClass.of(primary.hue)
    val companionClass = SpectralClass.of(companion.hue)

    // The barycenter sits closer to whichever star is "heavier" (bigger StarSize): each star's
    // own orbital radius is inversely proportional to its own mass stand-in, the two always
    // summing to BINARY_SEPARATION_DP -- a fixed total separation, only the split point moves.
    val totalMass = primarySize.diameter.value + companionSize.diameter.value
    val primaryOrbitDp = BINARY_SEPARATION_DP * (companionSize.diameter.value / totalMass)
    val companionOrbitDp = BINARY_SEPARATION_DP * (primarySize.diameter.value / totalMass)

    val transition = rememberInfiniteTransition(label = "binary")
    val angleDegrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(BINARY_ORBIT_MILLIS, easing = LinearEasing)),
        label = "binary-angle",
    )
    val radians = angleDegrees * PI / 180.0
    val companionRadians = radians + PI

    val density = LocalDensity.current
    val primaryOrbitPx = with(density) { primaryOrbitDp.dp.toPx() }
    val companionOrbitPx = with(density) { companionOrbitDp.dp.toPx() }
    val largerDiameter = maxOf(primarySize.diameter, companionSize.diameter)
    val outerDiameter = largerDiameter + (BINARY_SEPARATION_DP * 2).dp

    Box(modifier = Modifier.size(outerDiameter), contentAlignment = Alignment.Center) {
        Offer(act = primary.act) {
            Box(
                modifier = Modifier
                    .tell(owesTell, weight)
                    .clickable { engage() }
                    .size(primarySize.diameter)
                    .offset {
                        IntOffset(
                            x = (primaryOrbitPx * cos(radians)).toInt(),
                            y = (primaryOrbitPx * sin(radians)).toInt(),
                        )
                    }
                    .clip(CircleShape)
                    .background(primaryClass.color),
            )
        }
        Offer(act = companion.act) {
            Box(
                modifier = Modifier
                    .tell(owesTell, weight)
                    .clickable { engage() }
                    .size(companionSize.diameter)
                    .offset {
                        IntOffset(
                            x = (companionOrbitPx * cos(companionRadians)).toInt(),
                            y = (companionOrbitPx * sin(companionRadians)).toInt(),
                        )
                    }
                    .clip(CircleShape)
                    .background(companionClass.color),
            )
        }
    }
}
