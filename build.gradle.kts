plugins {
    kotlin("jvm") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "dev.mr3n"
version = "1.0-SNAPSHOT"

val ktorVersion = "2.2.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.2.0-SNAPSHOT")
    implementation("net.dv8tion:JDA:5.0.0-beta.5")
    implementation("com.google.cloud:google-cloud-firestore:3.9.0")
    compileOnly(files("libs/server-1.0-SNAPSHOT-all.jar"))
}

kotlin {
    jvmToolchain(17)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    mergeServiceFiles()
}