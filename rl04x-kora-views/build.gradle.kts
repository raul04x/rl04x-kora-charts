// rl04x-kora-views: Traditional XML Views (KoraLineChartView, KoraBarChartView…).
// For projects not yet using Compose or mixing both UI systems.

plugins {
    id("kora-android-library")
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.rl04x.koracharts.views"

    buildFeatures {
        // ViewBinding for sample app internal Views
        viewBinding = true
    }
}

dependencies {
    // Internal module
    api(project(":rl04x-kora-core"))

    // Material for default styling of tooltips and legends
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso)
}

mavenPublishing {
    coordinates(
        groupId = "io.github.raul04x",
        artifactId = "rl04x-kora-views",
        version = libs.versions.kora.get()
    )
    pom {
        name.set("Kora Charts Views")
        description.set("XML View components for Kora Charts")
        url.set("https://github.com/raul04x/rl04x-kora-charts")
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
    }
}
