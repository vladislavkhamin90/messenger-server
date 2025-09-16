import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.yourname"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.2")
    implementation("io.ktor:ktor-server-netty:2.3.2")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.2")
    implementation("io.ktor:ktor-serialization-gson:2.3.2")
    implementation("io.ktor:ktor-server-websockets:2.3.2")
    implementation("io.ktor:ktor-server-cors:2.3.2")
    implementation("ch.qos.logback:logback-classic:1.4.8")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("com.yourname.ApplicationKt")
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "com.yourname.ApplicationKt"))
        }
        archiveFileName.set("messenger-server.jar")
    }
}
