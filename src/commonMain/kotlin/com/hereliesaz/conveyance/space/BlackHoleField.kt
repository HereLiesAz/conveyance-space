package com.hereliesaz.conveyance.space

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.SubjectId
import com.hereliesaz.conveyance.compose.ActScope
import com.hereliesaz.conveyance.compose.Collection
import com.hereliesaz.conveyance.compose.Offer
import com.hereliesaz.conveyance.compose.tell

private val DEBRIS_DIAMETER = 8.dp

/**
 * One piece of matter in a [BlackHoleField] -- unlike [ComposableRequest], this carries its own
 * [act], since [Collection] needs every item independently addressable. [act]'s consequence is
 * being consumed: engaging it is what the host wires up to remove this [subject] from the list it
 * passes as `debris`, which is what actually triggers [Collection]'s own Ghost residue -- this
 * library never removes anything itself.
 */
data class DebrisRequest(
    val subject: SubjectId,
    val act: Act,
    val hue: String,
)

/**
 * A collapsing star and the matter around it, using Conveyance's own [Collection] primitive for
 * genuine cross-element consumption -- not the single self-contained composable
 * `space.star.collapse` is on its own. Each [DebrisRequest] carries its own [Act]; consuming one
 * is the host removing its subject from [debris], and [Collection] renders the framework's own
 * Ghost residue for it -- Conveyance's own real motion doing the "sucking up," not this element's
 * chrome merely implying it. This is the same pattern
 * [com.hereliesaz.conveyance.bacterium.PredatorColony] (`conveyance-bacterium`) uses for genuine
 * predator/prey, applied here to genuine black-hole/debris.
 *
 * This is **not** a [Templates.registry] entry, for the same reason `PredatorColony` isn't:
 * every composable manifest element carries exactly one `act` (azphalt `spec/composable.md`), and
 * [Collection] inherently needs a caller-owned list of items each with its *own* act -- a shape
 * this library's single-element [ComposableRequest] can't express. A host wires this up directly.
 */
@Composable
fun BlackHoleField(
    star: ComposableRequest,
    debris: List<DebrisRequest>,
    /** Spawns a new piece of nearby matter -- [Collection]'s own required "where new things come from" control. */
    spawn: Act,
    modifier: Modifier = Modifier,
) {
    Box(contentAlignment = Alignment.Center) {
        Collection(
            items = debris,
            creator = spawn,
            key = { it.subject },
            modifier = modifier,
            creatorContent = { SpawnControl() },
            item = { item -> DebrisChip(item) },
        )
        Collapse(star)
    }
}

@Composable
private fun ActScope.SpawnControl() {
    Box(
        modifier = Modifier
            .tell(owesTell, weight)
            .clickable { engage() }
            .size(10.dp)
            .clip(CircleShape)
            .background(Color(0xFF7E97A6)),
    )
}

/** One piece of matter's chrome -- small, tinted by its own [SpectralClass], [Offer]-backed by its own [DebrisRequest.act]. */
@Composable
private fun DebrisChip(debris: DebrisRequest) {
    val color = SpectralClass.of(debris.hue).color
    Offer(act = debris.act) {
        Box(
            modifier = Modifier
                .tell(owesTell, weight)
                .clickable { engage() }
                .padding(2.dp)
                .size(DEBRIS_DIAMETER)
                .clip(CircleShape)
                .background(color),
        )
    }
}
