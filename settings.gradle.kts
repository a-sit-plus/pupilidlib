System.setProperty("KOTEST_NO_ASP_HELPER", "true")

pluginManagement {
    includeBuild("vclib/conventions-vclib")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://raw.githubusercontent.com/a-sit-plus/gradle-conventions-plugin/mvn/repo")
    }
}
includeBuild("vclib") {
    dependencySubstitution {
        substitute(module("at.asitplus.wallet:vclib")).using(project(":vclib"))
    }
}

rootProject.name = "pupilidlib"
include(":pupilidlib")
