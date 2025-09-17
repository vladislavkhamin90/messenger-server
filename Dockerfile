# Build stage
FROM gradle:8.5-jdk17-alpine as builder

WORKDIR /app
COPY . .

# Устанавливаем shadow plugin и собираем
RUN gradle shadowJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Копируем JAR файл из builder stage
COPY --from=builder /app/build/libs/messenger-server.jar .

EXPOSE 8080
CMD ["java", "-jar", "messenger-server.jar"]
