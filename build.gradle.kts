import org.jbake.app.Oven
import org.jbake.app.configuration.JBakeConfigurationFactory


plugins {
    kotlin("jvm") version "1.6.0"
    application
    id("net.researchgate.release") version "2.8.1"
}
buildscript {
    dependencies {
        classpath("org.jbake:jbake-core:2.6.7")
        classpath("io.pebbletemplates:pebble:3.1.5")
    }
}

group = "de.hanno"

repositories {
    mavenCentral()
}
dependencies {
    implementation("io.ktor:ktor-server-netty:1.6.7")
    implementation("ch.qos.logback:logback-classic:1.2.7")
}

val sourceFolder = project.rootDir.resolve("src/jbake")
val destinationFolder = project.buildDir.resolve("jbake")
val docsSubFolder = rootProject.rootDir.resolve("docs")

val bake by tasks.registering {
    inputs.dir(sourceFolder)
    group = "build"

    doFirst {
        destinationFolder.mkdir()
        val config = JBakeConfigurationFactory()
            .createDefaultJbakeConfiguration(sourceFolder, destinationFolder, true)
        Oven(config).bake()
    }
}

val servePreview by tasks.registering(JavaExec::class) {
    group = "serve"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("MainKt")
    args = listOf(destinationFolder.absolutePath)
}
val serveRelease by tasks.registering(JavaExec::class) {
    group = "serve"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("MainKt")
    args = listOf(docsSubFolder.absolutePath, "8081")
}

val compileToDocs by tasks.registering {
    group = "release"
    dependsOn(bake)
    mustRunAfter(tasks.clean)
    doFirst {
        docsSubFolder.deleteRecursively()
        docsSubFolder.mkdir()
        destinationFolder.copyRecursively(docsSubFolder)
    }
}

release {
    failOnCommitNeeded = false
    failOnUnversionedFiles = false
}
val addDocs by tasks.registering(Exec::class) {
    mustRunAfter(compileToDocs)
    workingDir = rootDir
    commandLine("git", "add", "docs")
}

tasks.beforeReleaseBuild {
    dependsOn(tasks.clean, compileToDocs, addDocs)
}