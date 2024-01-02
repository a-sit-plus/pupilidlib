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

startParameter.excludedTaskNames+="transformNativeMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+="transformAppleMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+="transformIosMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+="transformNativeTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+="transformAppleTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+="transformIosTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib:transformNativeMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib:transformAppleMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib:transformIosMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib:transformNativeTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib:transformAppleTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib:transformIosTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-aries:transformNativeMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-aries:transformAppleMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-aries:transformIosMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-aries:transformNativeTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-aries:transformAppleTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-aries:transformIosTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-openid:transformNativeMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-openid:transformAppleMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-openid:transformIosMainCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-openid:transformNativeTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-openid:transformAppleTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS
startParameter.excludedTaskNames+=":vclib:vclib-openid:transformIosTestCInteropDependenciesMetadataForIde" //disable broken import on non-macOS