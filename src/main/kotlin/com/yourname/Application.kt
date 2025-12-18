import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(
        Netty, 
        port = 8080, 
        host = "0.0.0.0"
    ) {
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Привет! Сервер работает! ✅")
        }
        
        get("/health") {
            call.respond(mapOf("status" to "OK"))
        }
        
        post("/register") {
            call.respondText("Регистрация пока не работает")
        }
    }
}
