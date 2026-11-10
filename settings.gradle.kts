pluginManagement {
    includeBuild("conventions-vclib")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://raw.githubusercontent.com/a-sit-plus/gradle-conventions-plugin/mvn/repo")
    }
}

rootProject.name = "pupilidlib"
include(":pupilidlib")
include(":vclib")
