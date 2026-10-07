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

// Task helper: imprime el árbol de módulos del proyecto
tasks.register("printModuleTree") {
    doLast {
        println("\n📦 Kora Charts — módulos")
        println("  :rl04x-kora-core    → Engine, renderer, animation, modelo de datos")
        println("  :rl04x-kora-compose → Composables para Jetpack Compose")
        println("  :rl04x-kora-views   → Views XML tradicionales")
        println("  :sample             → App de demo\n")
    }
}
