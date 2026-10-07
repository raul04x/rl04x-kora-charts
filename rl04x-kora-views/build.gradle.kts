// rl04x-kora-views: Views XML tradicionales (KoraLineChartView, KoraBarChartView…).
// Para proyectos que todavía no usan Compose o que mezclan ambos sistemas.

plugins {
    id("kora-android-library")
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

android {
    namespace = "com.rl04x.koracharts.views"

    buildFeatures {
        // ViewBinding para los Views internos de la app de sample
        viewBinding = true
    }
}

dependencies {
    // Módulo propio
    api(project(":rl04x-kora-core"))

    // Material para default styling de tooltips y leyendas
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
        groupId    = "io.github.raul04x",
        artifactId = "rl04x-kora-views",
        version    = "0.1.0-alpha01"
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
