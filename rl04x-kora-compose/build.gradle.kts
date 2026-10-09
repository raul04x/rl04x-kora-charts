// rl04x-kora-compose: Composables ready for Jetpack Compose.
// Depends on rl04x-kora-core; adds Compose integration.

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
    // Internal module
    api(project(":rl04x-kora-core"))

    // Compose BOM — unified versions without conflicts
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.bundles.compose.ui)

    // Tooling debug-only — excluded from release AAR
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
        version    = libs.versions.kora.get()
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
