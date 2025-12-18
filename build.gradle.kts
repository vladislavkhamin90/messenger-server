plugins {
    kotlin("jvm") version "1.9.22" 
    application  
}

repositories {
    mavenCentral() 
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.8")
    implementation("io.ktor:ktor-server-netty:2.3.8")
    
    implementation("io.ktor:ktor-server-content-negotiation:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")
    
    implementation("io.ktor:ktor-server-cors:2.3.8")
    
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    implementation("at.favre.lib:bcrypt:0.10.2")
}

application {
    mainClass.set("ApplicationKt")
}
