package com.hereliesaz.conveyance.space

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ActState
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Gate
import com.hereliesaz.conveyance.Outcome
import com.hereliesaz.conveyance.Refusal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `Templates.kt` had no tests at all, despite an adversarial audit having already found and fixed
 * three real defects here: none of the four templates distinguished [ActState.Blocked]/
 * [ActState.Refused] from the other states (`Pulsar` ignored act state entirely, and
 * `MoonLoading` rendered `Ready`, `Blocked`, `Settled`, and `Refused` pixel-identically); the
 * `Collapse` bloom-to-collapse fill snapped from white-75%-opaque straight to solid black the
 * instant progress crossed the halfway point; and `MoonLoading`'s indeterminate-to-determinate
 * transition used to teleport the moon instead of animating from wherever the spin last visibly
 * was. All three are pinned here.
 *
 * The state-distinguishability tests really compose a template, engage its act toward a real
 * `Blocked`/`Refused`/`Settled` terminal state, and read what `Modifier.semantics {
 * stateDescription = ... }` reports back -- the same harness `conveyance-h2g2`'s own
 * `TemplatesTest` uses for its own click-wiring regression. The bloom/collapse-continuity and
 * moon-seed-angle math is pinned directly against the pure functions [Templates.kt] extracts
 * from each composable for exactly this reason, following the same non-composable-function
 * pattern `conveyance-compose`'s own `Collection.kt` (`resolveSlots`/`pruneLikeness`) already
 * uses to keep this kind of logic testable without composing anything.
 */
@OptIn(ExperimentalTestApi::class)
class TemplatesTest {

    // ----- the registry --------------------------------------------------------------------

    @Test
    fun `the registry exposes exactly the four documented templateIds`() {
        assertEquals(
            setOf("space.star.system", "space.moon.loading", "space.star.pulsar", "space.star.collapse"),
            Templates.registry.keys,
        )
    }

    // ----- pure math: the Collapse bloom-to-collapse continuity fix ------------------------

    /**
     * The exact numbers the README cites: bloom ends at `bloomT = 1f` (white, 0.75 alpha);
     * collapse starts at `colorRampT = 0f`. If the fix is real, those two frames must be
     * identical -- not merely close -- since one is rendered the instant after the other.
     */
    @Test
    fun `the bloom phase's last frame and the collapse phase's first frame are identical`() {
        val base = Color(0xFF112233)
        assertEquals(collapseBloomColor(base, 1f), collapseRampColor(0f))
        assertEquals(collapseBloomAlpha(1f), collapseRampAlpha(0f))
        // Concretely: both are white at 0.75 alpha, the number the README documents.
        assertEquals(Color.White, collapseBloomColor(base, 1f))
        assertEquals(0.75f, collapseBloomAlpha(1f))
    }

    @Test
    fun `the bloom phase starts fully opaque in the star's own color`() {
        val base = Color(0xFF112233)
        assertEquals(base, collapseBloomColor(base, 0f))
        assertEquals(1f, collapseBloomAlpha(0f))
    }

    @Test
    fun `the collapse phase ends fully opaque black`() {
        assertEquals(Color.Black, collapseRampColor(1f))
        assertEquals(1f, collapseRampAlpha(1f))
    }

    @Test
    fun `collapse alpha never pops back up as colorRampT advances`() {
        val samples = listOf(0f, 0.1f, 0.3f, 0.5f, 0.75f, 1f)
        val alphas = samples.map(::collapseRampAlpha)
        for (i in 1 until alphas.size) {
            assertTrue(alphas[i] >= alphas[i - 1], "alpha regressed at colorRampT=${samples[i]}: $alphas")
        }
    }

    // ----- pure math: the MoonLoading teleport fix ------------------------------------------

    /**
     * The exact defect: leaving indeterminate spin used to jump straight to the determinate
     * target. The fix seeds from wherever the spin last visibly was.
     */
    @Test
    fun `leaving indeterminate spin seeds from the spin's own last angle`() {
        assertEquals(217f, moonSeedAngle(wasIndeterminate = true, spinAngle = 217f))
    }

    @Test
    fun `already-determinate transitions never re-seed -- they animate onward from where they are`() {
        assertNull(moonSeedAngle(wasIndeterminate = false, spinAngle = 217f))
    }

    // ----- pure math: BinaryStarSystem's own orbit math also lives here, per Templates.kt's ---
    // ----- established internal-pure-function pattern; BinaryStars.kt has its own test file. --

    // ----- state-color mappings, directly -------------------------------------------------

    @Test
    fun `MoonLoading's four documented states each get a distinct color`() {
        val ready = moonStateColor(ActState.Ready)
        val blocked = moonStateColor(ActState.Blocked(gate("g1")))
        val refused = moonStateColor(ActState.Refused(Refusal.Denied))
        val settled = moonStateColor(ActState.Settled)
        assertEquals(4, setOf(ready, blocked, refused, settled).size, "expected 4 distinct colors, got $ready/$blocked/$refused/$settled")
    }

    @Test
    fun `Pulsar's core color distinguishes Blocked and Refused from its resting white`() {
        val blocked = pulsarCoreColor(ActState.Blocked(gate("g2")))
        val refused = pulsarCoreColor(ActState.Refused(Refusal.Denied))
        val resting = pulsarCoreColor(ActState.Ready)
        assertEquals(3, setOf(blocked, refused, resting).size)
        assertEquals(Color.White, resting)
    }

    @Test
    fun `isBlockedRing is true for Blocked and only Blocked`() {
        assertTrue(isBlockedRing(ActState.Blocked(gate("g3"))))
        assertTrue(!isBlockedRing(ActState.Ready))
        assertTrue(!isBlockedRing(ActState.Settled))
        assertTrue(!isBlockedRing(ActState.Refused(Refusal.Denied)))
        assertTrue(!isBlockedRing(ActState.Yielding()))
    }

    @Test
    fun `refusedTint is the base color at rest and the marker color at full flash`() {
        val base = Color(0xFF445566)
        assertEquals(base, refusedTint(base, 0f))
        assertNotEquals(base, refusedTint(base, 1f))
        // Fully flashed is the same regardless of base -- it is the marker color itself.
        assertEquals(refusedTint(Color.Blue, 1f), refusedTint(Color.Green, 1f))
    }

    private fun gate(id: String) = Gate(id, ElementId("gate:$id")) { false }

    // ----- the fix, end to end: real composition, real engagement, real semantics ----------

    private val templatesByTag: List<Pair<String, @Composable (ComposableRequest) -> Unit>> = listOf(
        "space.star.system.core" to { r: ComposableRequest -> StarSystem(r) },
        "space.moon.loading.planet" to { r: ComposableRequest -> MoonLoading(r) },
        "space.pulsar.core" to { r: ComposableRequest -> Pulsar(r) },
        "space.collapse.body" to { r: ComposableRequest -> Collapse(r) },
    )

    private fun request(act: Act) = ComposableRequest(act = act, hue = "G", rank = "primary", scale = "lead")

    private fun stateDescriptionAfter(
        tag: String,
        template: @Composable (ComposableRequest) -> Unit,
        act: Act,
        engage: Boolean,
    ): String? {
        var result: String? = null
        runComposeUiTest {
            setContent { template(request(act)) }
            waitForIdle()
            if (engage) {
                onNodeWithTag(tag).performClick()
                waitForIdle()
            }
            result = onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)
        }
        return result
    }

    @Test
    fun `every template renders Ready distinctly at rest`() {
        templatesByTag.forEach { (tag, template) ->
            val act = Act.reveal("ready.$tag", ElementId("target.$tag"))
            assertEquals("ready", stateDescriptionAfter(tag, template, act, engage = false), tag)
        }
    }

    @Test
    fun `every template renders Blocked distinctly, not the same as Ready`() {
        templatesByTag.forEach { (tag, template) ->
            val act = Act.reveal(
                "blocked.$tag",
                ElementId("target.$tag"),
                requires = listOf(gate("blocked.$tag")),
            )
            assertEquals("blocked", stateDescriptionAfter(tag, template, act, engage = true), tag)
        }
    }

    @Test
    fun `every template renders Refused distinctly, not the same as Blocked or Settled`() {
        templatesByTag.forEach { (tag, template) ->
            val act = Act.reveal(
                "refused.$tag",
                ElementId("target.$tag"),
                perform = { Outcome.Failed(Refusal.Denied) },
            )
            assertEquals("refused", stateDescriptionAfter(tag, template, act, engage = true), tag)
        }
    }

    @Test
    fun `every template renders Settled distinctly, not the same as Ready`() {
        templatesByTag.forEach { (tag, template) ->
            val act = Act.reveal(
                "settled.$tag",
                ElementId("target.$tag"),
                perform = { Outcome.Done },
            )
            assertEquals("settled", stateDescriptionAfter(tag, template, act, engage = true), tag)
        }
    }

    // ----- the `.clickable { engage() }` wiring, for real -----------------------------------

    @Test
    fun `every registry template engages its act when tapped`() {
        // Templates.registry (Templates.kt) and templatesByTag above are both declared in the
        // same star/moon/pulsar/collapse order, so they line up positionally; the registry's own
        // lambdas wrap each template in a fresh closure, which rules out matching by identity.
        val registryTemplates = Templates.registry.entries.toList()
        assertEquals(templatesByTag.size, registryTemplates.size)
        registryTemplates.zip(templatesByTag).forEach { (entry, tagged) ->
            val (id, template) = entry
            val (tag, _) = tagged
            var engaged = false
            runComposeUiTest {
                setContent { template(request(Act.reveal("engage.$id", ElementId("target.$id"), perform = { engaged = true; Outcome.Done }))) }
                waitForIdle()
                onNodeWithTag(tag).performClick()
                waitForIdle()
            }
            assertTrue(engaged, "$id renders but never engages its act on tap.")
        }
    }
}
