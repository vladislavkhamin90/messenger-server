plugins {
    kotlin("jvm") version "1.9.0"
    application
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
    jar {
        manifest {
            attributes("Main-Class" to "com.yourname.messenger.ApplicationKt")
        }
        
        from(configurations.runtimeClasspath.get().map { 
            if (it.isDirectory) it else zipTree(it) 
        })
        
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
