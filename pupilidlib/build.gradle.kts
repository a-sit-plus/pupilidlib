import at.asitplus.gradle.*
import at.asitplus.gradle.at.asitplus.gradle.addTestExtensions
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("de.infix.testBalloon")
    kotlin("plugin.serialization")
    id("at.asitplus.gradle.vclib-conventions")
}

/* required for maven publication */
val artifactVersion = extra["artifactVersion"] as String
group = "at.asitplus.wallet"
version = artifactVersion

kotlin {
    jvm()
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
        namespace = "at.asitplus.wallet.pupilidlib"
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunnerArguments["timeout_msec"] = "2400000"
            instrumentationRunnerArguments["TESTBALLOON_REPORTING_PATH_LIMIT"] = "400"
            managedDevices {
                localDevices {
                    create("pixelAVD").apply {
                        device = "Pixel 4"
                        apiLevel = 35
                        systemImageSource = "aosp-atd"
                    }
                }
            }
        }
        packaging.resources.excludes += listOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/licenses/*",
            "META-INF/LICENSE.*",
            "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
        )
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        /* Main source sets */
        commonMain {
            dependencies {
                commonImplementationAndApiDependencies()
                api(ktor("client-core"))
                api(ktor("client-logging"))
                api(ktor("client-serialization"))
                api(ktor("client-content-negotiation"))
                api(ktor("serialization-kotlinx-json"))
                api(libs.vclib)
                api(napier())
            }
        }
        iosMain {
            dependencies {
                api(ktor("client-darwin"))
            }
        }

        androidJvmMain {
            dependencies {
                api(bouncycastle("bcpkix"))
                api(libs.swagger.annotations)
                api(ktor("client-okhttp"))
            }
        }

        /* Test source sets */
        commonTest {
            dependencies {
                implementation(ktor("client-mock"))
                addTestExtensions()
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.nimbus.jose.jwt)
            }
        }


    }
}

exportXCFramework(
    "PupilIdKMM",
    transitiveExports = false,
    additionalExports = arrayOf(
        kmmresult(),
        libs.vclib.get(),
        datetime(),
        napier()
    )
)

val gitLabProjectId = extra["gitLabProjectId"] as String
val gitLabGroupId = extra["gitLabGroupId"] as String

repositories {
    mavenLocal()
    gitlab(groupId = gitLabGroupId.toInt()) accessTokenFrom extra
    mavenCentral()
}


publishing {
    repositories {
        mavenLocal()
        gitLab(projectId = gitLabProjectId.toInt())
        maven {
            name = "GitHubPackages"
            url = uri(
                "https://maven.pkg.github.com/" +
                    providers.environmentVariable("GITHUB_REPOSITORY").orElse("a-sit-plus/pupilidlib").get().lowercase()
            )
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }
    }
}
