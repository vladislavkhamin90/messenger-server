package com.yourname.messenger

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.server.request.*
import java.time.Duration
import java.util.*

// Data classes
data class User(
    val id: String,
    val username: String,
    val email: String,
    val password: String
)

data class RegisterRequest(val username: String, val email: String, val password: String)
data class RegisterResponse(val success: Boolean, val message: String)
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val user: UserResponse, val success: Boolean)
data class UserResponse(val id: String, val username: String, val email: String)

// In-memory хранилище
object UserRepository {
    private val users = mutableListOf<User>()
    private val tokens = mutableMapOf<String, String>()

    init {
        // Тестовый пользователь
        users.add(User(
            id = UUID.randomUUID().toString(),
            username = "testuser",
            email = "test@example.com",
            password = "password123"
        ))
    }

    fun userExists(username: String): Boolean = users.any { it.username == username }

    fun addUser(user: User) {
        users.add(user)
    }

    fun getUserByUsername(username: String): User? = users.find { it.username == username }

    fun getAllUsersExcept(excludeUsername: String): List<UserResponse> {
        return users
            .filter { it.username != excludeUsername }
            .map { UserResponse(it.id, it.username, it.email) }
    }

    fun saveToken(token: String, username: String) {
        tokens[token] = username
    }

    fun getUsernameByToken(token: String): String? = tokens[token]

    fun validateUser(username: String, password: String): Boolean {
        val user = getUserByUsername(username)
        return user != null && user.password == password
    }
}

fun Application.module() {
    install(CORS) {
        anyHost()
    }

    install(ContentNegotiation) {
        gson()
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

        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
                
                if (request.username.isEmpty() || request.email.isEmpty() || request.password.isEmpty()) {
                    call.respond(RegisterResponse(false, "Все поля обязательны"))
                    return@post
                }

                if (UserRepository.userExists(request.username)) {
                    call.respond(RegisterResponse(false, "Пользователь уже существует"))
                    return@post
                }

                val user = User(
                    id = UUID.randomUUID().toString(),
                    username = request.username,
                    email = request.email,
                    password = request.password
                )

                UserRepository.addUser(user)
                call.respond(RegisterResponse(true, "Регистрация успешна"))

            } catch (e: Exception) {
                call.respond(RegisterResponse(false, "Ошибка: ${e.message}"))
            }
        }

        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                
                if (UserRepository.validateUser(request.username, request.password)) {
                    val user = UserRepository.getUserByUsername(request.username)!!
                    val token = UUID.randomUUID().toString()
                    UserRepository.saveToken(token, request.username)

                    call.respond(LoginResponse(
                        token, 
                        UserResponse(user.id, user.username, user.email), 
                        true
                    ))
                } else {
                    call.respond(LoginResponse("", UserResponse("", "", ""), false))
                }

            } catch (e: Exception) {
                call.respond(LoginResponse("", UserResponse("", "", ""), false))
            }
        }

        get("/users") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val username = UserRepository.getUsernameByToken(token)

            if (username == null) {
                call.respondText("Неавторизованный доступ")
            } else {
                val users = UserRepository.getAllUsersExcept(username)
                call.respond(users)
            }
        }

        webSocket("/chat") {
            val token = call.request.queryParameters["token"] ?: ""
            val username = UserRepository.getUsernameByToken(token) ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }

            send("{\"type\": \"connected\", \"message\": \"Welcome $username\"}")
            
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    send("{\"type\": \"message\", \"from\": \"$username\", \"content\": \"$text\"}")
                }
            }
        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    println("Starting server on port $port")

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}
