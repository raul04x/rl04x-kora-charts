// rl04x-kora-compose: Composables listos para Jetpack Compose.
// Depende de rl04x-kora-core; agrega solo el pegamento de Compose.

plugins {
    id("kora-android-library")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.rl04x.koracharts.compose"

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Módulo propio
    api(project(":rl04x-kora-core"))

    // Compose BOM — versiones unificadas sin conflictos
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose.ui)

    // Tooling solo en debug — no entra en el AAR de release
    debugImplementation(libs.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.compose.test.junit4)
    debugImplementation(libs.compose.test.manifest)
}

mavenPublishing {
    coordinates(
        groupId    = "io.github.raul04x",
        artifactId = "rl04x-kora-compose",
        version    = "0.1.0-alpha01"
    )
    pom {
        name.set("Kora Charts Compose")
        description.set("Jetpack Compose components for Kora Charts")
        url.set("https://github.com/raul04x/rl04x-kora-charts")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
    }
}
