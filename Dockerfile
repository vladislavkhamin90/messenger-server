FROM gradle:8.5-jdk17-alpine as builder

WORKDIR /app
COPY . .

# Собираем JAR
RUN gradle jar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем JAR файл
COPY --from=builder /app/build/libs/messenger-server-1.0.0.jar ./app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
