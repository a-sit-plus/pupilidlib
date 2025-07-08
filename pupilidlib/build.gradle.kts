import at.asitplus.gradle.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("at.asitplus.gradle.vclib-conventions")
}

/* required for maven publication */
val artifactVersion: String by extra
group = "at.asitplus.wallet"
version = "$artifactVersion"

val vcLibVersion: String by extra
val swagger: String by extra



kotlin {
    jvm()
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
                api("at.asitplus.wallet:vclib:$vcLibVersion")
                api(napier())
            }
        }
        iosMain {
            dependencies {
                api(ktor("client-darwin"))
            }
        }

        jvmMain {
            dependencies {
                api(bouncycastle("bcpkix"))
                api("io.swagger.core.v3:swagger-annotations:$swagger")
                api(ktor("client-okhttp"))
            }
        }

        /* Test source sets */
        commonTest {
            dependencies {
                implementation(ktor("client-mock"))
            }
        }
        jvmTest {
            dependencies {
                implementation("com.nimbusds:nimbus-jose-jwt:${VcLibVersions.Jvm.`jose-jwt`}")
            }
        }


    }
}

exportXCFramework(
    "PupilIdKMM",
    transitiveExports = false,
    additionalExports = arrayOf(
        "at.asitplus:kmmresult:${AspVersions.kmmresult}",
        "at.asitplus.wallet:vclib:$vcLibVersion",
        datetime(),
        napier()
    )
)


configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
}

val gitLabProjectId: String by extra
val gitLabGroupId: String by extra

repositories {
    mavenLocal()
    gitlab(groupId = gitLabGroupId.toInt()) accessTokenFrom extra
    mavenCentral()
}


publishing {
    repositories {
        mavenLocal()
        gitLab(projectId = gitLabProjectId.toInt())
    }
}
