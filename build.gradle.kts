// Root build file — no dependencies here, only top-level plugin declarations.
// Each module owns its own build.gradle.kts.
plugins {
    id("com.android.library") apply false
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.maven.publish) apply false
}

// Task helper: prints the project module tree
tasks.register("printModuleTree") {
    doLast {
        println("\n📦 Kora Charts — modules")
        println("  :rl04x-kora-core    → Engine, renderer, animation, data model")
        println("  :rl04x-kora-compose → Composables for Jetpack Compose")
        println("  :rl04x-kora-views   → Traditional XML Views")
        println("  :sample             → Demo app\n")
    }
}
