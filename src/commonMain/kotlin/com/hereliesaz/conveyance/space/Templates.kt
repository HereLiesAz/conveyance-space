package com.hereliesaz.conveyance.space

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.compose.Offer
import com.hereliesaz.conveyance.compose.tell
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Every template attaches Modifier.tell(owesTell, weight).clickable { engage() } to its outermost
// shape -- the wiring Conveyance's own demo (conveyance-demo/.../Gallery.kt) uses at every real
// Offer call site. Without it a template still renders correctly but is inert: nothing engages
// the act on tap, so ActState can never leave Ready through this template alone.

/** A visible, full-opacity marker for [ActState.Blocked] -- never reduced opacity
 *  ([ActState.Blocked]'s own doc: "Never renders as reduced opacity"), so every template that
 *  needs to mark a blocked state distinctly from [ActState.Ready] does it with this color, not
 *  with alpha. */
private val BLOCKED_MARKER_COLOR = Color(0xFFE8B339)

/** A visible marker for [ActState.Refused] -- distinct from [BLOCKED_MARKER_COLOR] so a person
 *  can tell "can't yet" from "tried and failed" at a glance. */
private val REFUSED_MARKER_COLOR = Color(0xFFE0453B)

/**
 * What a `kind: "composable"` `.azp` package's `elements[]` entry (azphalt `spec/composable.md`)
 * supplies once a host has resolved it against this library's [Templates.registry] and built the
 * live [Act] the element performs. `hue` names a [SpectralClass] (`"O"`.."M"`, real black-body
 * color by temperature); `rank` (reusing the manifest's `hue`-adjacent semantic-rank vocabulary,
 * same as `conveyance-expressive`) names a [StarSize] -- the two independent axes of a real
 * Hertzsprung-Russell diagram. [planetCount]/[hasMoon] are only used by `space.star.system`.
 */
data class ComposableRequest(
    val act: Act,
    val hue: String,
    val rank: String,
    val scale: String,
    val label: String? = null,
    val planetCount: Int = 3,
    /** Whether the outermost planet has its own moon orbiting it, in `space.star.system`. */
    val hasMoon: Boolean = false,
)

/**
 * The space composable-set's template registry -- what a `templateId` resolves against once this
 * artifact is linked at build time. A host looks a `templateId` up here and calls the matching
 * function with the manifest's declared token values.
 */
object Templates {
    val registry: Map<String, @Composable (ComposableRequest) -> Unit> = mapOf(
        "space.star.system" to { request -> StarSystem(request) },
        "space.moon.loading" to { request -> MoonLoading(request) },
        "space.star.pulsar" to { request -> Pulsar(request) },
        "space.star.collapse" to { request -> Collapse(request) },
    )
}

private const val PLANET_BASE_RADIUS_DP = 36f
private const val PLANET_SPACING_DP = 20f
private const val PLANET_ORBIT_REFERENCE_MILLIS = 3200
private const val MOON_BASE_RADIUS_DP = 12f
private const val MOON_ORBIT_REFERENCE_MILLIS = 900

/**
 * A star that reveals its planets on engagement -- [com.hereliesaz.conveyance.Consequence.Reveal]
 * read literally. [ComposableRequest.planetCount] planets orbit continuously once revealed, each
 * animating outward from the star's own center to its real orbital radius rather than simply
 * appearing there, and each period derived from [Orbits.periodMillisFor] (Kepler's third law) --
 * an outer planet takes longer to complete an orbit because it has farther to travel and moves
 * slower doing it, not because an author chose a bigger number for it.
 *
 * [ComposableRequest.hasMoon] gives the *outermost* planet its own moon, orbiting *it* rather
 * than the star -- a genuinely separate orbital system, timed independently
 * ([MOON_ORBIT_REFERENCE_MILLIS], much faster than any planet's period, since a moon orbits its
 * planet's far smaller mass, not the star's) rather than sharing the star-relative Kepler
 * reference [PLANET_ORBIT_REFERENCE_MILLIS] uses. The moon's screen position is the planet's own
 * *current* position plus its own orbital offset -- it moves because its parent planet moves,
 * genuinely nested motion, not a fixed decoration riding along.
 *
 * Planets are revealed only for [ActState.Yielding]/[ActState.Settled] -- [ActState.Ready] and
 * [ActState.Blocked] both stay unrevealed, since neither is "engaged and working." A blocked
 * star still marks itself, at full opacity ([ActState.Blocked]'s own doc: "Never renders as
 * reduced opacity") via [BLOCKED_MARKER_COLOR]'s ring; a refused one flashes
 * [REFUSED_MARKER_COLOR] once before settling back.
 */
@Composable
fun StarSystem(request: ComposableRequest) {
    val spectralClass = SpectralClass.of(request.hue)
    val starSize = StarSize.of(request.rank)
    val density = LocalDensity.current
    val outerDiameter = starSize.diameter + (PLANET_BASE_RADIUS_DP + PLANET_SPACING_DP * request.planetCount).dp * 2

    Box(modifier = Modifier.size(outerDiameter), contentAlignment = Alignment.Center) {
        Offer(act = request.act) {
            val revealed = state is ActState.Yielding || state is ActState.Settled
            val revealProgress = remember { Animatable(0f) }
            LaunchedEffect(revealed) {
                revealProgress.animateTo(if (revealed) 1f else 0f, tween(if (revealed) 700 else 300))
            }
            val refusedFlash = remember { Animatable(0f) }
            LaunchedEffect(state) {
                if (state is ActState.Refused) {
                    refusedFlash.snapTo(1f)
                    refusedFlash.animateTo(0f, tween(500))
                }
            }

            repeat(request.planetCount) { index ->
                // Keyed so each planet's remembered orbital state stays tied to its own index
                // if planetCount ever changes between recompositions, rather than Compose's
                // plain positional memoization silently reassigning slot N's animation state
                // to whichever planet happens to occupy slot N after a count change.
                key(index) {
                    val orbitRadiusPx = with(density) {
                        (PLANET_BASE_RADIUS_DP + index * PLANET_SPACING_DP).dp.toPx()
                    }
                    val referenceRadiusPx = with(density) { PLANET_BASE_RADIUS_DP.dp.toPx() }
                    val periodMillis = Orbits.periodMillisFor(
                        orbitRadiusPx,
                        referenceRadiusPx,
                        PLANET_ORBIT_REFERENCE_MILLIS,
                    )
                    val transition = rememberInfiniteTransition(label = "planet-$index")
                    val angleDegrees by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
                        label = "angle-$index",
                    )
                    val radians = angleDegrees * PI / 180.0
                    val currentRadiusPx = orbitRadiusPx * revealProgress.value
                    val planetX = currentRadiusPx * cos(radians)
                    val planetY = currentRadiusPx * sin(radians)
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .offset { IntOffset(x = planetX.toInt(), y = planetY.toInt()) }
                            .clip(CircleShape)
                            .background(Color(0xFF7E97A6)),
                    )

                    if (request.hasMoon && index == request.planetCount - 1) {
                        // A single moon at one fixed radius has nothing to scale relative to --
                        // Kepler's law compares *different* orbits around the *same* body, and
                        // there's only one orbit here, so MOON_ORBIT_REFERENCE_MILLIS is used
                        // directly rather than routed through Orbits.periodMillisFor for a ratio
                        // that would always come out to 1.0 anyway.
                        val moonOrbitRadiusPx = with(density) { MOON_BASE_RADIUS_DP.dp.toPx() }
                        val moonTransition = rememberInfiniteTransition(label = "moon")
                        val moonAngleDegrees by moonTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                tween(MOON_ORBIT_REFERENCE_MILLIS, easing = LinearEasing),
                            ),
                            label = "moon-angle",
                        )
                        val moonRadians = moonAngleDegrees * PI / 180.0
                        val moonCurrentRadiusPx = moonOrbitRadiusPx * revealProgress.value
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .offset {
                                    IntOffset(
                                        x = (planetX + moonCurrentRadiusPx * cos(moonRadians)).toInt(),
                                        y = (planetY + moonCurrentRadiusPx * sin(moonRadians)).toInt(),
                                    )
                                }
                                .clip(CircleShape)
                                .background(Color(0xFFB8BCC0)),
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .tell(owesTell, weight)
                    .clickable { engage() }
                    .size(starSize.diameter)
                    .clip(CircleShape)
                    .background(lerp(spectralClass.color, REFUSED_MARKER_COLOR, refusedFlash.value))
                    .let { base ->
                        if (state is ActState.Blocked) base.border(2.dp, BLOCKED_MARKER_COLOR, CircleShape) else base
                    },
            )
        }
        request.label?.let {
            BasicText(text = it, modifier = Modifier.padding(top = outerDiameter / 2 + 8.dp))
        }
    }
}

private const val MOON_SPIN_MILLIS = 1400
private const val ANGLE_SETTLE_MILLIS = 260
private val READY_MOON_COLOR = Color(0xFFC9C9C9)
private val SETTLED_MOON_COLOR = Color(0xFFFFFFFF)

/**
 * A loading indicator: a moon orbiting a planet. Indeterminate progress
 * ([com.hereliesaz.conveyance.ActState.Yielding] with a `null` extent) spins the moon
 * continuously at constant angular velocity -- the real indeterminate-spinner case, since there
 * is no known endpoint to point toward. Determinate progress (a non-null extent) maps the
 * fraction directly onto orbital angle: a moon at "12 o'clock" is a real, readable "done" the
 * way a percentage number alone isn't at a glance. The switch from indeterminate to determinate
 * (or back to rest) animates from wherever the spin last visibly was, rather than teleporting.
 *
 * The moon's own color, not just its position, carries [ActState.Blocked]/[ActState.Refused]/
 * [ActState.Settled] -- [ActState.Ready] and [ActState.Settled] both park the moon at 12 o'clock,
 * so position alone can't tell "not started" from "done"; color does.
 */
@Composable
fun MoonLoading(request: ComposableRequest) {
    val spectralClass = SpectralClass.of(request.hue)
    val planetDiameter = StarSize.of(request.rank).diameter * 0.7f
    val moonOrbitRadiusDp = planetDiameter.value / 2f + 10f
    val outerDiameter = planetDiameter + (moonOrbitRadiusDp.dp - planetDiameter / 2f) * 2 + 16.dp

    Box(modifier = Modifier.size(outerDiameter), contentAlignment = Alignment.Center) {
        Offer(act = request.act) {
            val transition = rememberInfiniteTransition(label = "moon-spin")
            val spinAngle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(MOON_SPIN_MILLIS, easing = LinearEasing)),
                label = "spin",
            )
            val yieldingExtent = (state as? ActState.Yielding)?.extent
            val indeterminate = state is ActState.Yielding && yieldingExtent == null
            // -90 degrees is 12 o'clock in this offset's coordinates (angle 0 would be 3
            // o'clock); a full lap from there and back reads as "started home, ended home."
            val restAngle = -90f
            val determinateTarget = if (state is ActState.Yielding && yieldingExtent != null) {
                restAngle + yieldingExtent * 360f
            } else {
                restAngle
            }
            val settledAngle = remember { Animatable(restAngle) }
            var wasIndeterminate by remember { mutableStateOf(false) }
            LaunchedEffect(indeterminate, determinateTarget) {
                if (indeterminate) {
                    wasIndeterminate = true
                    return@LaunchedEffect
                }
                if (wasIndeterminate) {
                    // Seed from wherever the indeterminate spin last visibly was, so this doesn't
                    // teleport from an arbitrary spin position straight to the new target.
                    settledAngle.snapTo(spinAngle)
                    wasIndeterminate = false
                }
                settledAngle.animateTo(determinateTarget, tween(ANGLE_SETTLE_MILLIS))
            }
            val angleDegrees = if (indeterminate) spinAngle else settledAngle.value
            val radians = angleDegrees * PI / 180.0
            val moonOrbitRadiusPx = with(LocalDensity.current) { moonOrbitRadiusDp.dp.toPx() }
            val moonColor = when (state) {
                is ActState.Blocked -> BLOCKED_MARKER_COLOR
                is ActState.Refused -> REFUSED_MARKER_COLOR
                ActState.Settled -> SETTLED_MOON_COLOR
                else -> READY_MOON_COLOR
            }

            Box(
                modifier = Modifier
                    .tell(owesTell, weight)
                    .clickable { engage() }
                    .size(planetDiameter)
                    .clip(CircleShape)
                    .background(spectralClass.color),
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset {
                        IntOffset(
                            x = (moonOrbitRadiusPx * cos(radians)).toInt(),
                            y = (moonOrbitRadiusPx * sin(radians)).toInt(),
                        )
                    }
                    .clip(CircleShape)
                    .background(moonColor),
            )
        }
    }
}

private const val PULSAR_ROTATION_MILLIS = 900
private const val PULSAR_REST_ANGLE = 0f

/**
 * A rotating lighthouse beam, the real mechanism behind a pulsar's regular flash -- not a pulsing
 * scale/alpha loop, an actual beam sweeping past a fixed viewing angle twice per rotation (real
 * pulsars beam from two opposing magnetic poles, which is also why a beam spanning straight
 * through the star, rotating around its own center, is the accurate shape here rather than a
 * single ray). The core brightens each time the beam sweeps past. For an "urgent"/"just
 * happened" indicator -- [com.hereliesaz.conveyance.Meaning.Heat], the meaning
 * [com.hereliesaz.conveyance.Channel.Chroma] carries.
 *
 * Rotation, and the flash it drives, run only while [ActState.Yielding] -- the "urgent, actively
 * happening" state this indicator exists for. [ActState.Ready]/[ActState.Blocked] hold the beam
 * still at rest; [ActState.Settled] freezes it at wherever it last swept to, core held at full
 * brightness (done, not still flashing); [ActState.Blocked]/[ActState.Refused] mark the core in
 * [BLOCKED_MARKER_COLOR]/[REFUSED_MARKER_COLOR] at full opacity rather than white.
 */
@Composable
fun Pulsar(request: ComposableRequest) {
    val coreDiameter = StarSize.of(request.rank).diameter * 0.5f
    val beamLength = coreDiameter * 3f

    Box(modifier = Modifier.size(beamLength), contentAlignment = Alignment.Center) {
        Offer(act = request.act) {
            val rotating = state is ActState.Yielding
            val transition = rememberInfiniteTransition(label = "pulsar")
            val spinningAngle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(PULSAR_ROTATION_MILLIS, easing = LinearEasing)),
                label = "beam",
            )
            val settledAngle = remember { mutableFloatStateOf(PULSAR_REST_ANGLE) }
            LaunchedEffect(state) {
                if (state is ActState.Settled) settledAngle.floatValue = spinningAngle
            }
            val beamAngle = when {
                rotating -> spinningAngle
                state is ActState.Settled -> settledAngle.floatValue
                else -> PULSAR_REST_ANGLE
            }
            val phase = beamAngle % 180f
            val distanceFromSweep = min(abs(phase), 180f - abs(phase))
            val flash = if (rotating) (1f - distanceFromSweep / 90f).coerceIn(0f, 1f).let { it * it } else 0f
            val coreColor = when (state) {
                is ActState.Blocked -> BLOCKED_MARKER_COLOR
                is ActState.Refused -> REFUSED_MARKER_COLOR
                else -> Color.White
            }
            val coreBaseAlpha = if (state is ActState.Settled) 1f else 0.55f

            Box(
                modifier = Modifier
                    .tell(owesTell, weight)
                    .clickable { engage() }
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(beamLength, 2.dp)
                        .graphicsLayer { rotationZ = beamAngle }
                        .background(Color.White.copy(alpha = if (rotating) 0.30f else 0f)),
                )
                Box(
                    modifier = Modifier
                        .size(coreDiameter)
                        .graphicsLayer {
                            val boost = 1f + flash * 0.6f
                            scaleX = boost
                            scaleY = boost
                        }
                        .clip(CircleShape)
                        .background(coreColor.copy(alpha = (coreBaseAlpha + flash * 0.45f).coerceAtMost(1f))),
                )
            }
        }
    }
}

private const val BLOOM_PHASE_END = 0.5f
private val COLLAPSE_CORE_DIAMETER = 6.dp

/** Where the bloom phase's own final frame leaves off (white, [BLOOM_END_ALPHA] opaque) -- the
 *  collapse phase's fill starts here and ramps to opaque black over its own first third, rather
 *  than snapping straight to solid black the instant [BLOOM_PHASE_END] is crossed. */
private const val BLOOM_END_ALPHA = 0.75f
private const val COLLAPSE_COLOR_RAMP_FRACTION = 3f

/**
 * Supernova, then black hole -- a star's own chrome carrying a screen-scale destructive
 * transition, driven entirely by its act's state (never by moving anything else; motion across
 * elements is Conveyance's own business, not this library's). The first half of
 * [com.hereliesaz.conveyance.ActState.Yielding]'s progress is the bloom: the star swells rapidly
 * and whitens, with a faint expanding shockwave ring. Past the midpoint it collapses hard, down
 * to a small black core -- the real outcome of a massive star's core-collapse supernova -- with a
 * thin accretion ring in its own original color left glowing around the remnant once
 * [com.hereliesaz.conveyance.ActState.Settled]. The fill's own color/alpha ramps continuously
 * across the [BLOOM_PHASE_END] boundary (starting from the bloom's own last frame, white at
 * [BLOOM_END_ALPHA]) rather than snapping straight to opaque black.
 *
 * [com.hereliesaz.conveyance.ActState.Ready]/[com.hereliesaz.conveyance.ActState.Blocked] both
 * render the plain resting star -- a blocked one full-opacity ringed in [BLOCKED_MARKER_COLOR]
 * rather than dimmed. [com.hereliesaz.conveyance.ActState.Refused] flashes
 * [REFUSED_MARKER_COLOR] before settling back to resting.
 *
 * This is one star collapsing on its own, not an actual black hole consuming separately
 * addressed matter -- for that, see [BlackHoleField] (`BlackHoleField.kt`), which uses
 * Conveyance's real `Collection` primitive for genuine cross-element consumption, the same
 * pattern `conveyance-bacterium`'s `PredatorColony` uses for predator/prey; it isn't a
 * [Templates.registry] entry because [Collection] needs a caller-owned list of per-item acts, a
 * shape a single `ComposableRequest` can't express.
 */
@Composable
fun Collapse(request: ComposableRequest) {
    val spectralClass = SpectralClass.of(request.hue)
    val starSize = StarSize.of(request.rank)
    val boxDiameter = starSize.diameter * 3f

    Box(modifier = Modifier.size(boxDiameter), contentAlignment = Alignment.Center) {
        Offer(act = request.act) {
            val progress = when (state) {
                is ActState.Settled -> 1f
                is ActState.Yielding -> yielding ?: 0f
                else -> 0f
            }
            val refusedFlash = remember { Animatable(0f) }
            LaunchedEffect(state) {
                if (state is ActState.Refused) {
                    refusedFlash.snapTo(1f)
                    refusedFlash.animateTo(0f, tween(500))
                }
            }
            val clickModifier = Modifier.tell(owesTell, weight).clickable { engage() }

            if (progress <= 0f) {
                Box(
                    modifier = clickModifier
                        .size(starSize.diameter)
                        .clip(CircleShape)
                        .background(lerp(spectralClass.color, REFUSED_MARKER_COLOR, refusedFlash.value))
                        .let { base ->
                            if (state is ActState.Blocked) {
                                base.border(2.dp, BLOCKED_MARKER_COLOR, CircleShape)
                            } else {
                                base
                            }
                        },
                )
            } else if (progress < BLOOM_PHASE_END) {
                val bloomT = (progress / BLOOM_PHASE_END).coerceIn(0f, 1f)
                val diameter = lerpDp(starSize.diameter, boxDiameter, bloomT)
                val color = lerp(spectralClass.color, Color.White, bloomT)
                Box(
                    modifier = Modifier
                        .size(diameter * 1.6f)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = (1f - bloomT) * 0.5f), CircleShape),
                )
                Box(
                    modifier = clickModifier
                        .size(diameter)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 1f - bloomT * (1f - BLOOM_END_ALPHA))),
                )
            } else {
                val collapseT = ((progress - BLOOM_PHASE_END) / (1f - BLOOM_PHASE_END)).coerceIn(0f, 1f)
                val diameter = lerpDp(boxDiameter, COLLAPSE_CORE_DIAMETER, collapseT)
                if (collapseT > 0.3f) {
                    val ringAlpha = ((collapseT - 0.3f) / 0.7f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(COLLAPSE_CORE_DIAMETER * 4f)
                            .clip(CircleShape)
                            .border(1.5.dp, spectralClass.color.copy(alpha = ringAlpha * 0.7f), CircleShape),
                    )
                }
                // Continuous with the bloom phase's own final frame (white, BLOOM_END_ALPHA) at
                // collapseT = 0, ramping to opaque black by a third of the way through the
                // collapse -- no color/alpha pop at the BLOOM_PHASE_END boundary.
                val colorRampT = (collapseT * COLLAPSE_COLOR_RAMP_FRACTION).coerceIn(0f, 1f)
                Box(
                    modifier = clickModifier
                        .size(diameter)
                        .clip(CircleShape)
                        .background(
                            lerp(Color.White, Color.Black, colorRampT)
                                .copy(alpha = BLOOM_END_ALPHA + (1f - BLOOM_END_ALPHA) * colorRampT),
                        ),
                )
            }
        }
    }
}
