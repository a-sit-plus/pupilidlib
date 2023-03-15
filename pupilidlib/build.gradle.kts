import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkConfig
import java.io.FileInputStream
import java.util.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version PupilIdLibVersions.kotlin
    id("io.kotest.multiplatform") version PupilIdLibVersions.kotest
    id("maven-publish")
}

/* required for maven publication */
val artifactVersion: String by extra
group = "at.asitplus.wallet"
version = "$artifactVersion"

kotlin {

    val xcf = XCFrameworkConfig(project, "PupilIdKMM")

    ios {
        binaries.framework {
            baseName = "PupilIdKMM"
            export("at.asitplus:kmmresult:${PupilIdLibVersions.resultlib}")
            export("at.asitplus.wallet:vclib:${PupilIdLibVersions.vclib}")
            embedBitcode("bitcode")
            xcf.add(this)
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = "PupilIdKMM"
            export("at.asitplus:kmmresult:${PupilIdLibVersions.resultlib}")
            export("at.asitplus.wallet:vclib:${PupilIdLibVersions.vclib}")
            embedBitcode("bitcode")
            xcf.add(this)
        }
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = PupilIdLibVersions.Jvm.target
                freeCompilerArgs = listOf(
                    "-Xjsr305=strict"
                )
            }
        }

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs = listOf(
                    "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlin.time.ExperimentalTime",
                    "-opt-in=kotlin.RequiresOptIn",
                )
            }
        }
    }

    sourceSets {
        /* Main source sets */
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${PupilIdLibVersions.coroutines}")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${PupilIdLibVersions.serialization}")
                implementation("com.benasher44:uuid:${PupilIdLibVersions.uuid}")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${PupilIdLibVersions.datetime}")
                implementation("io.github.aakira:napier:${PupilIdLibVersions.napier}")
                api("at.asitplus.wallet:vclib:${PupilIdLibVersions.vclib}") {
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
                api("at.asitplus:kmmresult:${PupilIdLibVersions.resultlib}")
            }


        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosMain by getting
        val nativeMain by creating
        val jvmMain by getting

        /* Main hierarchy */
        iosMain.dependsOn(nativeMain)
        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)
        nativeMain.dependsOn(commonMain)
        jvmMain.dependsOn(commonMain)

        /* Test source sets */
        val commonTest by getting {
            dependencies {
                implementation("io.kotest:kotest-assertions-core:${PupilIdLibVersions.kotest}")
                implementation("io.kotest:kotest-framework-engine:${PupilIdLibVersions.kotest}")
            }
        }
        val iosX64Test by getting
        val iosArm64Test by getting
        val jvmTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:${PupilIdLibVersions.kotest}")
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
Properties().apply {
    kotlin.runCatching { load(FileInputStream(project.rootProject.file("local.properties"))) }
    forEach { (k, v) -> extra.set(k as String, v) }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)

}

val gitLabPrivateToken: String? by extra
val gitLabProjectId: String by extra
val gitLabGroupId: String by extra

repositories {
    maven(uri(layout.projectDirectory.dir("..").dir("vclib").dir("repo")))
    mavenLocal()
    if (System.getenv("CI_JOB_TOKEN") != null || gitLabPrivateToken != null) {
        maven {
            name = "gitlab"
            url = uri("https://gitlab.iaik.tugraz.at/api/v4/groups/$gitLabGroupId/-/packages/maven")
            if (gitLabPrivateToken != null) {
                credentials(HttpHeaderCredentials::class) {
                    name = "Private-Token"
                    value = gitLabPrivateToken
                }
            } else if (System.getenv("CI_JOB_TOKEN") != null) {
                credentials(HttpHeaderCredentials::class) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
    mavenCentral()
}


publishing {
    repositories {
        mavenLocal()
        if (System.getenv("CI_JOB_TOKEN") != null) {
            maven {
                name = "gitlab"
                url = uri("https://gitlab.iaik.tugraz.at/api/v4/projects/$gitLabProjectId/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
        maven {
            url = uri(layout.projectDirectory.dir("..").dir("repo"))
            name = "local"
        }
    }
}
tasks.getByName("publishAllPublicationsToLocalRepository") {
    doLast {
        copy {
            from(layout.projectDirectory.dir("..").dir("vclib").dir("repo"))
            into(layout.projectDirectory.dir("..").dir("repo"))
        }
    }
}
