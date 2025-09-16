# Используем официальный образ Java 17
FROM eclipse-temurin:17-jdk-alpine as builder

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файлы проекта
COPY . .

# Даем права на выполнение gradlew (если он есть)
RUN chmod +x ./gradlew

# Собираем JAR файл
RUN ./gradlew shadowJar

# Второй этап - минимальный образ
FROM eclipse-temurin:17-jre-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем собранный JAR из первого этапа
COPY --from=builder /app/build/libs/messenger-server.jar .

# Открываем порт
EXPOSE 8080

# Команда запуска
CMD ["java", "-jar", "messenger-server.jar"]
