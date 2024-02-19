pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    includeBuild("vclib/conventions-vclib")
}

includeBuild("vclib/kmp-crypto") {
    dependencySubstitution {
        substitute(module("at.asitplus.crypto:datatypes")).using(project(":datatypes"))
        substitute(module("at.asitplus.crypto:datatypes-jws")).using(project(":datatypes-jws"))
        substitute(module("at.asitplus.crypto:datatypes-cose")).using(project(":datatypes-cose"))
    }
}


includeBuild("vclib") {
    dependencySubstitution {
        substitute(module("at.asitplus.wallet:vclib")).using(project(":vclib"))
        substitute(module("at.asitplus.wallet:vclib-aries")).using(project(":vclib-aries"))
    }
}

rootProject.name = "pupilidlib"
include(":pupilidlib")