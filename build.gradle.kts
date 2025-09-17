plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.yourname.messenger"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-serialization-gson:2.3.0")
    implementation("io.ktor:ktor-server-websockets:2.3.0")
    implementation("io.ktor:ktor-server-cors:2.3.0")
    implementation("ch.qos.logback:logback-classic:1.4.7")
}

application {
    mainClass.set("com.yourname.messenger.ApplicationKt")
}

tasks {
    shadowJar {
        archiveBaseName.set("messenger-server")
        archiveVersion.set("")
        manifest {
            attributes(Pair("Main-Class", "com.yourname.messenger.ApplicationKt"))
        }
    }
}
