System.setProperty("TESTBALLOON_NO_ASP_HELPER", "true")

pluginManagement {
    includeBuild("vclib/conventions-vclib")
    repositories {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots") //Kotest snapshot for Kotlin 2.0.20 until new Kotest stable is released
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
includeBuild("vclib") {
    dependencySubstitution {
        substitute(module("at.asitplus.wallet:vclib")).using(project(":vclib"))
    }
}

rootProject.name = "pupilidlib"
include(":pupilidlib")
