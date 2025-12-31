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
    id("com.gradleup.shadow").version("9.2.2")
}

group = "org.vinerdream"
version = "1.5.3-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases")
}

val proguard: Configuration by configurations.creating

val citPaper = "org.vinerdream:CIT-paper:1.5.3-SNAPSHOT"
val paperAPI = "io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT"

dependencies {
    implementation(citPaper)
    implementation(paperAPI)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    proguard(citPaper)
    proguard(paperAPI)
    proguard("org.projectlombok:lombok:1.18.36")
    proguard("org.jetbrains:annotations:24.0.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.vinerdream.CitCliMain"
    }
}

tasks {
    shadowJar {
        exclude(
            "converter.yml",
            "languages.yml",
            "mechanics.yml",
            "sounds.yml",
            "custom_armor/**",
            "glyphs/**",
            "items/**",
            "messages/**",
            "pack/**",
            "shaders/**",
        )

        relocate("com.nexomc", "org.vinerdream.citPaper.libs.nexo")
        relocate("kotlin", "org.vinerdream.citPaper.libs.kotlin")
        relocate("team.unnamed", "org.vinerdream.citPaper.libs.unnamed")

        minimize()
    }
}

tasks.register<ProGuardTask>("proguard") {
    configuration(listOf(file("../proguard.pro"), file("proguard.pro")))

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

    outjars(layout.buildDirectory.file("libs/${project.name}-${project.version}-minified.jar"))
}
