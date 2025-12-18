// Самый простой сервер
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    // Запускаем сервер
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        routing {
            get("/") {
                call.respondText("🚀 Сервер работает на Render!")
            }
            
            get("/health") {
                call.respondText("✅ OK")
            }
            
            get("/test") {
                call.respondText("Тестовая страница")
            }
        }
    }.start(wait = true)
}
