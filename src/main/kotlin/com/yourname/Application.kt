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
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.time.Duration
import java.util.*

data class User(
    val id: String,
    val username: String,
    val email: String,
    val password: String
)

data class RegisterRequest(val username: String, val email: String, val password: String)
data class RegisterResponse(val success: Boolean, val message: String)
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val success: Boolean, val message: String?, val token: String?, val user: UserResponse?)
data class UserResponse(val id: String, val username: String, val email: String)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "message"
)

data class WebSocketMessage(
    val type: String,
    val from: String,
    val to: String? = null,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ChatRepository {
    private val messages = mutableListOf<ChatMessage>()
    private val connections = mutableMapOf<String, WebSocketSession>()
    private val userChannels = mutableMapOf<String, Channel<WebSocketMessage>>()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        println("💾 Сообщение сохранено: ${message.senderId} -> ${message.receiverId}: ${message.text}")
    }

    fun getMessagesBetweenUsers(user1: String, user2: String): List<ChatMessage> {
        return messages.filter { 
            (it.senderId == user1 && it.receiverId == user2) ||
            (it.senderId == user2 && it.receiverId == user1)
        }.sortedBy { it.timestamp }
    }

    fun getUserMessages(userId: String): List<ChatMessage> {
        return messages.filter { it.senderId == userId || it.receiverId == userId }
    }

    fun addConnection(userId: String, session: WebSocketSession) {
        connections[userId] = session
        userChannels[userId] = Channel(Channel.UNLIMITED)
        println("🔗 Пользователь $userId подключен к WebSocket")
    }

    fun removeConnection(userId: String) {
        connections.remove(userId)
        userChannels[userId]?.close()
        userChannels.remove(userId)
        println("🔗 Пользователь $userId отключен от WebSocket")
    }

    suspend fun sendMessageToUser(userId: String, message: WebSocketMessage) {
        val channel = userChannels[userId]
        if (channel != null) {
            channel.send(message)
        } else {
            println("⚠️ Канал для пользователя $userId не найден")
        }
    }

    fun getConnection(userId: String): WebSocketSession? {
        return connections[userId]
    }

    fun getConnectedUsers(): List<String> {
        return connections.keys.toList()
    }
}

object UserRepository {
    private val users = mutableListOf<User>()
    private val tokens = mutableMapOf<String, String>()
    private val usernameToId = mutableMapOf<String, String>()

    init {
        // Тестовый пользователь
        val testUser = User(
            id = UUID.randomUUID().toString(),
            username = "test",
            email = "test@example.com",
            password = "123456"
        )
        users.add(testUser)
        usernameToId[testUser.username] = testUser.id
        println("👤 Создан тестовый пользователь: ${testUser.username} (${testUser.id})")
    }

    fun userExists(username: String): Boolean = users.any { it.username == username }

    fun addUser(user: User) {
        users.add(user)
        usernameToId[user.username] = user.id
        println("👤 Зарегистрирован новый пользователь: ${user.username} (${user.id})")
    }

    fun getUserByUsername(username: String): User? = users.find { it.username == username }
    
    fun getUserById(id: String): User? = users.find { it.id == id }

    fun getAllUsersExcept(excludeUsername: String): List<UserResponse> {
        return users
            .filter { it.username != excludeUsername }
            .map { UserResponse(it.id, it.username, it.email) }
    }

    fun saveToken(token: String, username: String) {
        tokens[token] = username
        println("🔑 Токен сохранен для пользователя: $username")
    }

    fun getUsernameByToken(token: String): String? = tokens[token]
    
    fun getUserIdByUsername(username: String): String? = usernameToId[username]

    fun validateUser(username: String, password: String): Boolean {
        val user = getUserByUsername(username)
        return user != null && user.password == password
    }
}

fun Application.module() {
    install(CORS) {
        anyHost()
        allowCredentials = true
        allowNonSimpleContentTypes = true
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
        get("/") {
            call.respondText("Messenger Server is running! ✅\nConnected users: ${ChatRepository.getConnectedUsers()}")
        }

        get("/health") {
            call.respondText("OK")
        }

        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
                //!!!!!!!!!
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
                println("❌ Ошибка регистрации: ${e.message}")
                call.respond(RegisterResponse(false, "Ошибка: ${e.message}"))
            }
        }

        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                println("🔐 Попытка входа: ${request.username}")
                
                if (UserRepository.validateUser(request.username, request.password)) {
                    val user = UserRepository.getUserByUsername(request.username)!!
                    val token = UUID.randomUUID().toString()
                    UserRepository.saveToken(token, request.username)

                    call.respond(LoginResponse(
                        success = true,
                        message = "Вход выполнен успешно",
                        token = token,
                        user = UserResponse(user.id, user.username, user.email)
                    ))
                    println("✅ Успешный вход: ${request.username}")
                } else {
                    call.respond(LoginResponse(
                        success = false,
                        message = "Неверные учетные данные",
                        token = null,
                        user = null
                    ))
                    println("❌ Неверные учетные данные для: ${request.username}")
                }

            } catch (e: Exception) {
                println("❌ Ошибка входа: ${e.message}")
                call.respond(LoginResponse(
                    success = false,
                    message = "Ошибка сервера",
                    token = null,
                    user = null
                ))
            }
        }

        get("/users") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val username = UserRepository.getUsernameByToken(token)

            if (username == null) {
                call.respondText("Неавторизованный доступ", status = io.ktor.http.HttpStatusCode.Unauthorized)
            } else {
                val users = UserRepository.getAllUsersExcept(username)
                call.respond(users)
                println("📋 Пользователь $username запросил список контактов (${users.size} пользователей)")
            }
        }

        get("/messages/{userId}") {
            val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") ?: ""
            val currentUsername = UserRepository.getUsernameByToken(token)
            val otherUserId = call.parameters["userId"]

            if (currentUsername == null) {
                call.respondText("Неавторизованный доступ", status = io.ktor.http.HttpStatusCode.Unauthorized)
                return@get
            }

            if (otherUserId == null) {
                call.respondText("ID пользователя не указан", status = io.ktor.http.HttpStatusCode.BadRequest)
                return@get
            }

            val currentUser = UserRepository.getUserByUsername(currentUsername)
            val otherUser = UserRepository.getUserById(otherUserId)

            if (currentUser == null || otherUser == null) {
                call.respondText("Пользователь не найден", status = io.ktor.http.HttpStatusCode.NotFound)
                return@get
            }

            val messages = ChatRepository.getMessagesBetweenUsers(currentUser.id, otherUser.id)
            call.respond(messages)
            println("📨 Запрошена история сообщений между ${currentUser.username} и ${otherUser.username} (${messages.size} сообщений)")
        }

        webSocket("/chat") {
            val token = call.request.queryParameters["token"] ?: ""
            val username = UserRepository.getUsernameByToken(token)
            
            if (username == null) {
                println("❌ Неверный токен для WebSocket: $token")
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid token"))
                return@webSocket
            }

            val user = UserRepository.getUserByUsername(username) ?: run {
                println("❌ Пользователь не найден: $username")
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User not found"))
                return@webSocket
            }

            ChatRepository.addConnection(user.id, this)
            
            try {
                val welcomeMessage = WebSocketMessage(
                    type = "connected",
                    from = "server",
                    content = "Добро пожаловать в чат, $username!"
                )
                send(welcomeMessage.toJson())
                
                println("✅ Пользователь $username (${user.id}) подключился к WebSocket")

                val sendChannel = launch {
                    val userChannel = ChatRepository.userChannels[user.id]
                    if (userChannel != null) {
                        for (message in userChannel) {
                            try {
                                send(message.toJson())
                                println("📤 Отправлено сообщение пользователю $username: ${message.type}")
                            } catch (e: Exception) {
                                println("❌ Ошибка отправки сообщения пользователю $username: ${e.message}")
                                break
                            }
                        }
                    }
                }

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        try {
                            val text = frame.readText()
                            println("📨 Получено сообщение от $username: $text")
                            
                            val messageJson = kotlinx.serialization.json.Json.parseToJsonElement(text)
                            val to = messageJson.jsonObject["to"]?.jsonPrimitive?.content
                            val content = messageJson.jsonObject["content"]?.jsonPrimitive?.content
                            
                            if (to != null && content != null) {
                                val receiver = UserRepository.getUserById(to)
                                if (receiver != null) {
                                    val chatMessage = ChatMessage(
                                        senderId = user.id,
                                        receiverId = receiver.id,
                                        text = content
                                    )
                                    ChatRepository.addMessage(chatMessage)
                                    
                                    val wsMessage = WebSocketMessage(
                                        type = "message",
                                        from = user.username,
                                        to = receiver.id,
                                        content = content
                                    )
                                    
                                    if (ChatRepository.getConnection(receiver.id) != null) {
                                        ChatRepository.sendMessageToUser(receiver.id, wsMessage)
                                        println("📤 Сообщение отправлено пользователю ${receiver.username}")
                                    } else {
                                        println("⚠️ Пользователь ${receiver.username} не подключен к WebSocket")
                                    }
                                    
                                    val confirmation = WebSocketMessage(
                                        type = "message_sent",
                                        from = "server",
                                        content = "Сообщение доставлено"
                                    )
                                    send(confirmation.toJson())
                                } else {
                                    println("❌ Получатель не найден: $to")
                                }
                            }
                        } catch (e: Exception) {
                            println("❌ Ошибка обработки сообщения: ${e.message}")
                        }
                    }
                }
                
                sendChannel.cancel()
            } catch (e: Exception) {
                println("❌ Ошибка WebSocket для $username: ${e.message}")
            } finally {
                ChatRepository.removeConnection(user.id)
                println("🔗 Пользователь $username отключился от WebSocket")
            }
        }
    }
}

fun WebSocketMessage.toJson(): String {
    return """{"type":"$type","from":"$from","to":${to?.let { "\"$it\"" } ?: "null"},"content":"$content","timestamp":$timestamp}"""
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    println("🚀 Starting server on port $port")
    println("📡 WebSocket endpoint: ws://localhost:$port/chat")
    println("🔗 REST API: http://localhost:$port/")

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}
