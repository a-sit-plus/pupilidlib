plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    `maven-publish`
}

/* required for maven publication */
group = "at.asitplus"
version = "0.1"

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    android()

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

        /* Main hierarchy */
        iosMain.dependsOn(nativeMain)
        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)
        iosSimulatorArm64Main.dependsOn(iosMain)
        androidMain.dependsOn(commonMain)
        nativeMain.dependsOn(commonMain)

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
}

android {
    compileSdk = 31
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 31
    }
}
