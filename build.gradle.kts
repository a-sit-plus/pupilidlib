buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${PupilIdLibVersions.kotlin}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register<Delete>("clean") {
    doFirst { println("Cleaning all build files") }

    delete(rootProject.buildDir)
    delete(layout.projectDirectory.dir("repo"))
    doLast { println("Clean done") }
}

task<Exec>("purge") {
    dependsOn("clean")
    workingDir = layout.projectDirectory.dir("vclib").asFile
    commandLine("./gradlew", "clean")
    doFirst {
        println("descending into ${workingDir.absolutePath}")
        logger.lifecycle("Purging VcLib maven build")
    }
}
