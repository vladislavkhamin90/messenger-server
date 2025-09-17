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
import at.favre.lib.crypto.bcrypt.BCrypt

// Data classes
data class User(
    val id: String,
    val username: String,
    val email: String,
    val passwordHash: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class RegisterResponse(
    val success: Boolean,
    val message: String
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: UserResponse,
    val success: Boolean
)

data class UserResponse(
    val id: String,
    val username: String,
    val email: String
)

// In-memory хранилище (замените на базу данных в продакшене)
object UserRepository {
    private val users = mutableListOf<User>()
    private val tokens = mutableMapOf<String, String>() // token -> username

    fun userExists(username: String): Boolean {
        return users.any { it.username == username }
    }

    fun addUser(user: User) {
        users.add(user)
    }

    fun getUserByUsername(username: String): User? {
        return users.find { it.username == username }
    }

    fun getAllUsersExcept(excludeUsername: String): List<UserResponse> {
        return users
            .filter { it.username != excludeUsername }
            .map { UserResponse(it.id, it.username, it.email) }
    }

    fun saveToken(token: String, username: String) {
        tokens[token] = username
    }

    fun getUsernameByToken(token: String): String? {
        return tokens[token]
    }

    fun validateUser(username: String, password: String): Boolean {
        val user = getUserByUsername(username)
        return user != null && BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
    }
}

fun Application.module() {
    install(CORS) {
        anyHost()
        allowHeaders { true }
        allowMethods { true }
    }

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        // Health check
        get("/") {
            call.respondText("Messenger Server is running! ✅")
        }

        get("/health") {
            call.respondText("OK")
        }

        // Регистрация
        post("/register") {
            try {
                val registerRequest = call.receive<RegisterRequest>()

                // Валидация
                if (registerRequest.username.isEmpty() || registerRequest.email.isEmpty() || registerRequest.password.isEmpty()) {
                    call.respond(RegisterResponse(false, "Все поля обязательны для заполнения"))
                    return@post
                }

                if (registerRequest.password.length < 6) {
                    call.respond(RegisterResponse(false, "Пароль должен содержать минимум 6 символов"))
                    return@post
                }

                // Проверяем, нет ли уже такого пользователя
                if (UserRepository.userExists(registerRequest.username)) {
                    call.respond(RegisterResponse(false, "Пользователь уже существует"))
                    return@post
                }

                // Хэшируем пароль
                val hashedPassword = BCrypt.withDefaults().hashToString(12, registerRequest.password.toCharArray())

                // Сохраняем пользователя
                val user = User(
                    id = UUID.randomUUID().toString(),
                    username = registerRequest.username,
                    email = registerRequest.email,
                    passwordHash = hashedPassword
                )

                UserRepository.addUser(user)
                call.respond(RegisterResponse(true, "Регистрация успешна"))

            } catch (e: Exception) {
                call.respond(RegisterResponse(false, "Ошибка сервера: ${e.message}"))
            }
        }

        // Логин
        post("/login") {
            try {
                val loginRequest = call.receive<LoginRequest>()

                // Валидация
                if (loginRequest.username.isEmpty() || loginRequest.password.isEmpty()) {
                    call.respond(LoginResponse("", UserResponse("", "", ""), false))
                    return@post
                }

                // Проверяем пользователя
                if (UserRepository.validateUser(loginRequest.username, loginRequest.password)) {
                    val user = UserRepository.getUserByUsername(loginRequest.username)!!
                    
                    // Генерируем токен (в реальном приложении используйте JWT)
                    val token = UUID.randomUUID().toString()
                    UserRepository.saveToken(token, loginRequest.username)

                    val userResponse = UserResponse(user.id, user.username, user.email)
                    call.respond(LoginResponse(token, userResponse, true))
                } else {
                    call.respond(LoginResponse("", UserResponse("", "", ""), false))
                }

            } catch (e: Exception) {
                call.respond(LoginResponse("", UserResponse("", "", ""), false))
            }
        }

        // Получение списка пользователей (требует авторизацию)
        get("/users") {
            try {
                val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
                val username = UserRepository.getUsernameByToken(token)

                if (username == null) {
                    call.respondText("Неавторизованный доступ", status = io.ktor.http.HttpStatusCode.Unauthorized)
                    return@get
                }

                val users = UserRepository.getAllUsersExcept(username)
                call.respond(users)

            } catch (e: Exception) {
                call.respondText("Ошибка сервера: ${e.message}", status = io.ktor.http.HttpStatusCode.InternalServerError)
            }
        }

        // WebSocket для чата
        webSocket("/chat") {
            val token = call.request.queryParameters["token"] ?: ""
            val username = UserRepository.getUsernameByToken(token)

            if (username == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }

            send("{\"type\": \"connected\", \"message\": \"Добро пожаловать, $username!\"}")

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("Received from $username: $text")
                    
                    // Здесь можно добавить логику пересылки сообщений другим пользователям
                    send("{\"type\": \"message\", \"from\": \"$username\", \"content\": \"$text\"}")
                }
            }
        }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    println("Starting server on port $port")

    // Добавляем тестового пользователя для демонстрации
    val hashedPassword = BCrypt.withDefaults().hashToString(12, "password123".toCharArray())
    UserRepository.addUser(
        User(
            id = UUID.randomUUID().toString(),
            username = "testuser",
            email = "test@example.com",
            passwordHash = hashedPassword
        )
    )

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}
