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
                commonDependencies().forEach { dep -> api(dep) }
                api(serialization("cbor"))
                api(ktor("client-core"))
                api(ktor("client-logging"))
                api(ktor("client-serialization"))
                api(ktor("client-content-negotiation"))
                api(ktor("serialization-kotlinx-json"))
                api("at.asitplus.wallet:vclib-aries:$vcLibVersion") {
                    layout.projectDirectory.dir("..").dir("vclib").dir("repo").asFile.let {
                        if (it.exists() && it.isDirectory && it.listFiles()!!.isNotEmpty()) {
                            logger.info("assuming VcLib maven artifact present")
                        } else {
                            exec {
                                workingDir = layout.projectDirectory.dir("..").dir("vclib").asFile
                                println("descending into ${workingDir.absolutePath}")
                                logger.lifecycle("Rebuilding VcLib maven artifacts")
                                commandLine("./gradlew", "publishAllPublicationsToLocalRepository")
                            }
                        }
                    }
                }
                api("at.asitplus:kmmresult:${VcLibVersions.resultlib}")
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
                implementation(kotlin("reflect"))
            }
        }
        jvmTest {
            dependencies {
                implementation("com.nimbusds:nimbus-jose-jwt:${VcLibVersions.Jvm.`jose-jwt`}")
            }
        }
    }

}

exportIosFramework(
    "PupilIdKMM",
    "at.asitplus:kmmresult:${VcLibVersions.resultlib}",
    "at.asitplus.wallet:vclib-aries:$vcLibVersion",
    datetime(),
    napier()
)

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)

}

repositories {
    maven(uri(layout.projectDirectory.dir("..").dir("vclib").dir("repo")))
    mavenLocal()
    mavenCentral()
}


publishing {
    repositories {
        mavenLocal()
        maven {
            url = uri(layout.projectDirectory.dir("..").dir("repo"))
            name = "local"
        }
    }
}

listOf("publishAllPublicationsToLocalRepository", "publishJvmPublicationToLocalRepository").forEach { taskName ->
    tasks.getByName(taskName) {
        doLast {
            copy {
                from(layout.projectDirectory.dir("..").dir("vclib").dir("repo"))
                into(layout.projectDirectory.dir("..").dir("repo"))
            }
        }
    }
}
