pluginManagement {
    includeBuild("vclib/conventions-vclib")
}
includeBuild("vclib") {
    dependencySubstitution {
        substitute(module("at.asitplus.wallet:vclib")).using(project(":vclib"))
    }
}

rootProject.name = "pupilidlib"
include(":pupilidlib")
