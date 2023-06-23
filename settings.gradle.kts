pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    includeBuild("vclib/conventions-vclib")
}
includeBuild("vclib") {
    dependencySubstitution {
        substitute(module("at.asitplus.wallet:vclib-aries")).using(project(":vclib-aries"))
    }
}

rootProject.name = "pupilidlib"
include(":pupilidlib")