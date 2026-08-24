# conveyance-space

A composable-set library for [Conveyance](https://github.com/HereLiesAz/Conveyance): the Space
style -- shape, motion, and transformation modeled on real celestial mechanics. A star's planets
orbit into place when revealed; a loading indicator is a moon whipping around a planet; a pulsar's
flash is an actual rotating beam sweeping past, not a pulsing loop; a screen-scale destructive
transition is a real supernova collapsing into a black hole.

## What this is

Per [azphalt's `spec/composable.md`](https://github.com/HereLiesAz/azphalt/blob/main/spec/composable.md),
a `kind: "composable"` `.azp` package is a **pure header**: it names this artifact's Gradle
coordinates (`library.group` / `library.artifact`) and selects a `templateId`, `hue`,
`surface`, `scale`, and `act` from it. It carries no code of its own. This repository *is* the
artifact a composable package's `library` block points at -- the `.azp` package itself is
authored and published separately, wherever its author chooses; this repo does not need to hold
one.

Example composable manifest referencing this library:

```jsonc
{
  "azphalt": "0.1",
  "id": "com.hereliesaz.azphalt.example",
  "name": "Example",
  "version": "1.0.0",
  "kind": "composable",
  "license": "MIT",
  "compat": ">=0.1",
  "composable": {
    "library": { "group": "com.hereliesaz.conveyance", "artifact": "conveyance-space", "version": "0.1.0" },
    "elements": [
      { "id": "confirm-record", "templateId": "space.star.collapse", "hue": "K", "surface": "system", "scale": "lead", "act": "destroy", "jobs": ["confirms a destructive action"] }
    ]
  },
  "files": {}
}
```

## What's here

- **`SpectralClass`/`StarSize`** (`StellarClass.kt`) -- a star's two independent axes, the same
  two the real Hertzsprung-Russell diagram plots: `hue` selects a `SpectralClass` (the OBAFGKM
  sequence, real black-body color by temperature -- hotter is bluer, cooler is redder, not an
  invented palette); `rank` separately selects a `StarSize` (dwarf/main-sequence/giant), so a hot
  blue star can be a small dwarf or a giant just as a cool red one can, the way real stars
  actually combine the two.
- **`Orbits`** (`Orbits.kt`) -- Kepler's third law (`T ∝ r^1.5`): an orbiting body's period is
  *derived* from its orbital radius against one reference orbit, not chosen by eye. An outer
  planet orbits slower because it has farther to travel, the same reason Neptune's year is 165
  Earth years.
- **`Templates`** (`Templates.kt`) -- the `templateId` registry. Four templates:
  - `space.star.system` -- a star that reveals `planetCount` planets on engagement
    (`Consequence.Reveal` read literally): each animates outward from the star's own center to
    its real Kepler orbit and then keeps orbiting, rather than simply appearing there.
  - `space.moon.loading` -- a loading indicator: a moon orbiting a planet. Indeterminate progress
    (`ActState.Yielding` with a `null` extent) spins continuously; determinate progress maps the
    fraction directly onto orbital angle, so a moon back at 12 o'clock reads as "done" the way a
    percentage alone doesn't at a glance.
  - `space.star.pulsar` -- a real rotating lighthouse beam (real pulsars beam from two opposing
    magnetic poles, which is also why a beam spanning straight through the star is the accurate
    shape, not a single ray), the core flashing each time the beam sweeps past -- for an
    "urgent"/"just happened" indicator (`Meaning.Heat`, the meaning `Channel.Chroma` carries).
  - `space.star.collapse` -- supernova, then black hole: the first half of `ActState.Yielding`'s
    progress blooms and whitens the star with an expanding shockwave ring; past the midpoint it
    collapses hard to a small black core, leaving a thin accretion ring in the star's own
    original color glowing around the remnant once `Settled`. Self-contained -- one star
    collapsing on its own, not an actual black hole consuming separately addressed matter; see
    `BlackHoleField` below for that.
- **`BlackHoleField`/`DebrisRequest`** (`BlackHoleField.kt`) -- genuine cross-element consumption,
  built on Conveyance's own `Collection` primitive: `space.star.collapse` at the center, alongside
  a real population of independently addressed matter, each carrying its own `Act`. Consuming a
  piece is the host removing its `SubjectId` from the `debris` list it passes in; `Collection`
  renders the framework's own Ghost residue for it. This is the same pattern
  `conveyance-bacterium`'s `PredatorColony` uses for predator/prey, here giving the black hole a
  real "sucks up everything around it" rather than chrome merely implying it. **Not** a
  `Templates.registry` entry, for the same reason `PredatorColony` isn't: every composable
  manifest element carries exactly one `act` (azphalt `spec/composable.md`), and `Collection`
  inherently needs a caller-owned list of items each with its *own* act -- a shape the
  single-element `ComposableRequest` can't express. A host wires this up directly.

## Status

Four templates plus one genuine cross-element composable (`BlackHoleField`), each covering one of
the concept's named phenomena, but each is a single instance: `space.star.system` doesn't support
nested moons around its own planets, and `space.star.pulsar` isn't wired to `space.star.system`'s
star (a system's star can't currently *be* a pulsar).

## Using it

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.HereLiesAz:conveyance-space:main-SNAPSHOT")
}
```

Resolved via [JitPack](https://jitpack.io) directly from this repository -- `conveyance-core` and
`conveyance-compose` both apply `maven-publish`, which is all JitPack needs, so there is no
separate publish step to configure. Conveyance itself has no tagged release yet, so this artifact
and its upstream dependency on Conveyance both pin to `main-SNAPSHOT` for now; switch both to a
real tag once one exists.
