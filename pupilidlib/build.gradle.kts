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

exportIosFramework(
    "PupilIdKMM",
    "at.asitplus:kmmresult:${VcLibVersions.resultlib}",
    "at.asitplus.wallet:vclib-aries:$vcLibVersion",
    datetime(),
    napier()
)
kotlin {

    sourceSets {
        /* Main source sets */
        val commonMain by getting {
            dependencies {
                commonDependencies().forEach { dep -> api(dep) }
                api(serialization("cbor"))
                api(ktor("client-core"))
                api(ktor("client-logging"))
                api(ktor("client-serialization"))
                api(ktor("client-content-negotiation"))
                api(ktor("serialization-kotlinx-json"))
                api("at.asitplus.wallet:vclib:$vcLibVersion") {
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
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosMain by getting {
            dependencies {
                api(ktor("client-darwin"))
            }
        }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        val nativeMain by creating
        val jvmMain by getting {
            dependencies {
                api(bouncycastle("bcpkix"))
                api("io.swagger.core.v3:swagger-annotations:$swagger")
                api(ktor("client-okhttp"))
            }
        }

        /* Main hierarchy */
        iosMain.dependsOn(nativeMain)
        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)
        nativeMain.dependsOn(commonMain)
        jvmMain.dependsOn(commonMain)

        /* Test source sets */
        val commonTest by getting {
            dependencies {
                implementation(ktor("client-mock"))
            }
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val jvmTest by getting {
            dependencies {
                implementation("com.nimbusds:nimbus-jose-jwt:${VcLibVersions.Jvm.`jose-jwt`}")
            }
        }
        val iosTest by getting
        val nativeTest by creating

        /* Test hierarchy */
        iosTest.dependsOn(nativeTest)
        iosX64Test.dependsOn(iosTest)
        iosArm64Test.dependsOn(iosTest)
        nativeTest.dependsOn(commonTest)
    }

}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)

}

val gitLabProjectId: String by extra
val gitLabGroupId: String by extra

repositories {
    maven(uri(layout.projectDirectory.dir("..").dir("vclib").dir("repo")))
    mavenLocal()

    gitlab(groupId = gitLabGroupId.toInt()) accessTokenFrom extra

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
    gitLab(projectId = gitLabProjectId.toInt())
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
