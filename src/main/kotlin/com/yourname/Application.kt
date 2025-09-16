package com.yourname

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.server.plugins.cors.routing.*
import java.time.Duration

fun Application.module() {
    install(CORS) {
        anyHost()
        allowHeaders { true }
        allowMethods { true }
    }
    
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    routing {
        get("/") {
            call.respondText("Messenger Server is running! ✅")
        }
        
        get("/health") {
            call.respondText("OK")
        }
        
        webSocket("/chat") {
            send("{\"type\": \"connected\", \"message\": \"You are connected!\"}")
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("Received: $text")
                    send("{\"type\": \"echo\", \"message\": \"You said: $text\"}")
                }
            }
        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    println("Starting server on port $port")
    
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}