import org.jbake.app.Oven


plugins {
    kotlin("jvm") version "1.6.0"
    application
}
buildscript {
    dependencies {
        classpath("org.jbake:jbake-core:2.6.7")
        classpath("io.pebbletemplates:pebble:3.1.5")
    }
}

group = "de.hanno"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    implementation("io.ktor:ktor-server-netty:1.6.7")
    implementation("ch.qos.logback:logback-classic:1.2.7")
}

val bake by tasks.registering {
    group = "build"
    doFirst {
        val source = project.rootDir.resolve("src/jbake")
        val destination = project.buildDir.resolve("jbake").apply {
          mkdir()
        }
        Oven(source, destination, true).apply {
            setupPaths()
            bake()
        }
    }
}
application.mainClass.set("MainKt")
tasks.named("run", JavaExec::class) {
    args = listOf(project.buildDir.resolve("jbake").absolutePath)
}