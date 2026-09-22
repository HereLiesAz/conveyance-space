package com.hereliesaz.conveyance.space

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Outcome
import com.hereliesaz.conveyance.SubjectId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Invokes the node's `OnClick` semantics action directly, rather than dispatching a synthetic
 * pointer click at its on-screen bounds (`performClick()`). Two independently-orbiting,
 * infinitely-animating stars sharing one unpositioned `Box` (as [BinaryStarSystem] and
 * [BlackHoleField]'s `Collection`-driven layout both do) can legitimately overlap or shift
 * bounds between the frame a click is queued and the frame it lands -- `Modifier.tell`'s own
 * half-rep nudge alone moves a fresh element a few pixels the instant it is first composed. The
 * semantics action is what a real click ultimately invokes; calling it directly tests the same
 * wiring (`.clickable { engage() }`) without depending on where the node happens to be drawn.
 */
@OptIn(ExperimentalTestApi::class)
private fun SemanticsNodeInteraction.clickViaAction() {
    val invoked = fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
    assertTrue(invoked == true, "node has no OnClick semantics action to invoke")
}

/**
 * An adversarial audit's headline finding: [BlackHoleField]/[DebrisRequest] and
 * [BinaryStarSystem] had zero callers anywhere -- not in this repository, not in
 * `conveyance-demo`'s `StyleShowcase.kt`, which only iterates [Templates.registry]'s four
 * entries. Neither is a [Templates.registry] entry by design (see each composable's own doc
 * comment: [Collection] and two independent [Act]s both need a shape a single-element
 * [ComposableRequest] can't express), and this repository has no demo/gallery module of its own
 * to wire a visual call site into -- unlike `convey`'s `dev-app`/`android-dev-app` or
 * `conveyance-demo`. So this file is that first real call site: it actually composes both,
 * clicks their real controls, and checks the specific claims each composable's doc comment
 * makes -- [DebrisRequest] consumption really removing a subject, [BinaryStarSystem]'s two acts
 * really being independently addressable, and the orbital-radius math actually matching real
 * binary-star mechanics (the heavier star traces the tighter orbit) -- rather than merely
 * confirming the composables compile.
 */
@OptIn(ExperimentalTestApi::class)
class CrossElementCompositionTest {

    // ----- BinaryStarSystem: the pure orbital-radius math -----------------------------------

    @Test
    fun `the two orbit radii always sum to the total separation`() {
        val (primary, companion) = binaryOrbitRadii(primaryDiameter = 84f, companionDiameter = 28f, totalSeparation = 90f)
        assertEquals(90f, primary + companion, absoluteTolerance = 0.001f)
    }

    @Test
    fun `the heavier (larger-diameter) star traces the tighter orbit, like Sirius A`() {
        val (primaryOrbit, companionOrbit) = binaryOrbitRadii(primaryDiameter = 84f, companionDiameter = 28f, totalSeparation = 90f)
        assertTrue(primaryOrbit < companionOrbit, "the larger star (84) should orbit closer than the smaller one (28): $primaryOrbit vs $companionOrbit")
        // Concretely: 90 * 28/112 = 22.5, and 90 * 84/112 = 67.5.
        assertEquals(22.5f, primaryOrbit, absoluteTolerance = 0.01f)
        assertEquals(67.5f, companionOrbit, absoluteTolerance = 0.01f)
    }

    @Test
    fun `two equal-mass stars split the separation evenly`() {
        val (primary, companion) = binaryOrbitRadii(primaryDiameter = 48f, companionDiameter = 48f, totalSeparation = 90f)
        assertEquals(45f, primary, absoluteTolerance = 0.01f)
        assertEquals(45f, companion, absoluteTolerance = 0.01f)
    }

    @Test
    fun `non-positive diameters fall back to an even split rather than dividing by zero`() {
        val (primary, companion) = binaryOrbitRadii(primaryDiameter = 0f, companionDiameter = 0f, totalSeparation = 90f)
        assertEquals(45f, primary)
        assertEquals(45f, companion)
    }

    private fun assertEquals(expected: Float, actual: Float, absoluteTolerance: Float) {
        assertTrue(kotlin.math.abs(expected - actual) <= absoluteTolerance, "expected $expected, was $actual")
    }

    // ----- BinaryStarSystem: a real call site, each star independently engageable -----------

    @Test
    fun `BinaryStarSystem composes and each star engages only its own act`() = runComposeUiTest {
        var primaryEngaged = false
        var companionEngaged = false
        val primary = ComposableRequest(
            act = Act.reveal("binary.primary", ElementId("target.primary"), perform = { primaryEngaged = true; Outcome.Done }),
            hue = "O",
            rank = "primary",
            scale = "lead",
        )
        val companion = ComposableRequest(
            act = Act.reveal("binary.companion", ElementId("target.companion"), perform = { companionEngaged = true; Outcome.Done }),
            hue = "M",
            rank = "tertiary",
            scale = "lead",
        )
        setContent { BinaryStarSystem(primary, companion) }
        waitForIdle()

        onNodeWithTag("space.binary.primary").clickViaAction()
        waitForIdle()
        assertTrue(primaryEngaged, "clicking the primary star must engage the primary's own act")
        assertFalse(companionEngaged, "clicking the primary star must not engage the companion's act")

        onNodeWithTag("space.binary.companion").clickViaAction()
        waitForIdle()
        assertTrue(companionEngaged, "clicking the companion star must engage the companion's own act")
    }

    // ----- BlackHoleField: a real call site, genuine cross-element consumption --------------

    @Test
    fun `BlackHoleField's spawn control engages the spawn act`() = runComposeUiTest {
        var spawned = false
        val star = ComposableRequest(act = Act.reveal("field.star", ElementId("field")), hue = "K", rank = "primary", scale = "lead")
        val debris = listOf(
            DebrisRequest(SubjectId("d1"), Act.reveal("debris.d1", ElementId("field")), hue = "M"),
        )
        val spawn = Act.create("field.spawn", SubjectId("new"), ElementId("field"), perform = { spawned = true; Outcome.Done })

        setContent { BlackHoleField(star = star, debris = debris, spawn = spawn) }
        waitForIdle()
        onNodeWithTag("space.blackhole.spawn").clickViaAction()
        waitForIdle()
        assertTrue(spawned, "SpawnControl renders but never engages the spawn act")
    }

    /**
     * The exact claim [BlackHoleField]'s own doc comment makes: "Consuming a piece is the host
     * removing its `SubjectId` from the `debris` list it passes in." This composes a host that
     * really does that -- removes the subject from a live `debris` list from inside the debris
     * item's own act -- and confirms the item is genuinely gone afterward, not merely that the
     * click fired.
     */
    @Test
    fun `engaging a debris item's act really removes it from the field, per its own doc comment`() = runComposeUiTest {
        val subject = SubjectId("d1")
        val target = ElementId("field")
        var debris by mutableStateOf(
            listOf(
                DebrisRequest(
                    subject = subject,
                    act = Act.destroy(
                        "debris.destroy.d1",
                        subject,
                        target,
                        inverse = Act.create("debris.create.d1", subject, target),
                        perform = { Outcome.Done },
                    ),
                    hue = "M",
                ),
                DebrisRequest(SubjectId("d2"), Act.reveal("debris.d2", target), hue = "K"),
            ),
        )
        val star = ComposableRequest(act = Act.reveal("field.star", target), hue = "K", rank = "primary", scale = "lead")
        val spawn = Act.create("field.spawn", SubjectId("new"), target)

        setContent {
            // Stands in for what a real host wires up: engaging a debris act removes it from the
            // caller-owned list, the framework's own Collection then stops rendering it. This
            // isn't performed by BlackHoleField/DebrisRequest themselves (the doc comment is
            // explicit: "this library never removes anything itself") -- it's what a host does
            // when a piece's act settles, exercised here for real rather than merely declared.
            val current = debris
            BlackHoleField(
                star = star,
                debris = current.map { item ->
                    if (item.subject != subject) item else item.copy(
                        act = Act.destroy(
                            "debris.destroy.d1",
                            subject,
                            target,
                            inverse = Act.create("debris.create.d1", subject, target),
                            perform = { debris = debris.filterNot { it.subject == subject }; Outcome.Done },
                        ),
                    )
                },
                spawn = spawn,
            )
        }
        waitForIdle()

        onNodeWithTag("space.blackhole.debris.d1").assertExists()
        onNodeWithTag("space.blackhole.debris.d2").assertExists()

        onNodeWithTag("space.blackhole.debris.d1").clickViaAction()
        waitForIdle()

        onNodeWithTag("space.blackhole.debris.d1").assertDoesNotExist()
        onNodeWithTag("space.blackhole.debris.d2").assertExists()
    }
}
