plugins {
    id("java")
    id("io.github.goooler.shadow").version("8.1.8")
}

group = "org.vinerdream"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.vinerdream:CIT-paper:1.0-SNAPSHOT")
    implementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.vinerdream.CitCliMain"
    }
}