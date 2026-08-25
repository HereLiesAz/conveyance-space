pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    // `jvmToolchain(21)` (see gradle/libs.versions.toml) needs a real JDK 21 to compile against.
    // Without this, Gradle only ever looks at whatever JDKs happen to already be installed on the
    // machine running the build -- fine on a dev machine with one set up, but JitPack's build image
    // ships JDK 8-13 and nothing newer, and has no toolchain download repository configured on its
    // own. This plugin is exactly that: it lets Gradle fetch a matching JDK itself when none of the
    // locally installed ones satisfy the toolchain request, the same way it already does for the
    // Gradle wrapper's own distribution.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "conveyance-space"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
