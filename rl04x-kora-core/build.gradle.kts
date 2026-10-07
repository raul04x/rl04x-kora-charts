// rl04x-kora-core: engine, renderer, animation system, modelo de datos, touch handling.
// No depende de Compose ni de Views — es puro Canvas + Android SDK.

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

    // AndroidX mínimo — solo lo que Canvas necesita
    implementation(libs.androidx.core.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
}

// Metadata para Maven Central
mavenPublishing {
    coordinates(
        groupId    = "com.rl04x.koracharts",
        artifactId = "rl04x-kora-core",
        version    = "0.1.0-alpha01"
    )
    pom {
        name.set("Kora Charts Core")
        description.set("Core engine, renderer and animation system for Kora Charts")
        url.set("https://github.com/koracharts/kora-charts")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
    }
}
