import proguard.gradle.ProGuardTask

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.6.1")
    }
}

plugins {
    id("java")
    id("io.github.goooler.shadow").version("8.1.8")
}

group = "org.vinerdream"
version = "1.4.4-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

val proguard: Configuration by configurations.creating

val citPaper = "org.vinerdream:CIT-paper:1.4.4-SNAPSHOT"
val paperAPI = "io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT"

dependencies {
    implementation(citPaper)
    implementation(paperAPI)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    proguard(citPaper)
    proguard(paperAPI)
    proguard("org.projectlombok:lombok:1.18.36")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.vinerdream.CitCliMain"
    }
}

task<ProGuardTask>("proguard") {
    configuration(file("proguard.pro"))

    injars(tasks.named<Jar>("shadowJar").flatMap { it.archiveFile })

    listOf(
        "java.base",
        "java.desktop",
        "java.logging",
        "java.management",
        "java.sql",
        "jdk.unsupported"
    ).forEach { module -> libraryjars("${System.getProperty("java.home")}/jmods/${module}") }

    configurations.named("proguard").get().resolvedConfiguration.resolvedArtifacts.forEach { dependency -> libraryjars(dependency.file) }

    verbose()

    outjars(layout.buildDirectory.file("libs/${project.name}-${project.version}-all-minified.jar"))
}
