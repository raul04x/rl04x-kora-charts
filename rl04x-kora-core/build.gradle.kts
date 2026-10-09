// rl04x-kora-core: engine, renderer, animation system, data model, touch handling.
// Does not depend on Compose or Views — pure Canvas + Android SDK.

plugins {
    id("kora-android-library")
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.rl04x.koracharts.core"
}

dependencies {
    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.android)

    // Minimal AndroidX — only what Canvas requires
    implementation(libs.androidx.core.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
}

// Metadata for Maven Central
mavenPublishing {
    coordinates(
        groupId    = "io.github.raul04x",
        artifactId = "rl04x-kora-core",
        version    = libs.versions.kora.get()
    )
    pom {
        name.set("Kora Charts Core")
        description.set("Core engine, renderer and animation system for Kora Charts")
        url.set("https://github.com/raul04x/rl04x-kora-charts")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
    }
}
