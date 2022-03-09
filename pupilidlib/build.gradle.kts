import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("maven-publish")
}

/* required for maven publication */
group = "at.asitplus.wallet"
version = "0.1.0-SNAPSHOT"

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    android {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = listOf(
                    "-Xjsr305=strict"
                )
            }
        }
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = listOf(
                    "-Xjsr305=strict"
                )
            }
        }

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        /* Main source sets */
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
                implementation("io.github.aakira:napier:2.3.0")
                implementation("io.ktor:ktor-client-core:2.0.0-beta-1")
                implementation("io.ktor:ktor-client-serialization:2.0.0-beta-1")
                implementation("io.ktor:ktor-client-content-negotiation:2.0.0-beta-1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.0.0-beta-1")
            }
        }
        val iosX64Main by getting 
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.0.0-beta-1")
            }
        }
        val iosMain by creating {
            dependencies {
                implementation("io.ktor:ktor-client-ios:2.0.0-beta-1")
            }
        }
        val nativeMain by creating
        val jvmMain by getting {
            dependencies {
                implementation("io.swagger.core.v3:swagger-annotations:2.1.12")
            }
        }

        /* Main hierarchy */
        iosMain.dependsOn(nativeMain)
        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)
        iosSimulatorArm64Main.dependsOn(iosMain)
        androidMain.dependsOn(commonMain)
        nativeMain.dependsOn(commonMain)
        jvmMain.dependsOn(commonMain)

        /* Test source sets */
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val iosX64Test by getting 
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val androidTest by getting
        val iosTest by creating
        val nativeTest by creating

        /* Test hierarchy */
        iosTest.dependsOn(nativeTest)
        iosX64Test.dependsOn(iosTest)
        iosArm64Test.dependsOn(iosTest)
        iosSimulatorArm64Test.dependsOn(iosTest)
        androidTest.dependsOn(commonTest)
        nativeTest.dependsOn(commonTest)
    }

    XCFramework().let {
        ios {
            binaries.framework {
                baseName = "PupilIdKMM"
                it.add(this)
            }
        }
    }

}

android {
    compileSdk = 31
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 31
    }
}

val gitLabPrivateToken: String? by extra
val gitLabProjectId: String by extra
val gitLabGroupId: String by extra

repositories {
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
    }
}
