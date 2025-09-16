# Используем официальный образ Java 17
FROM eclipse-temurin:17-jdk-alpine as builder

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем сначала только файлы конфигурации для кэширования
COPY build.gradle.kts settings.gradle.kts ./

# Копируем исходный код
COPY src ./src

# Устанавливаем Gradle и собираем проект
RUN apk add --no-cache bash
RUN wget https://services.gradle.org/distributions/gradle-8.3-bin.zip -O gradle.zip
RUN unzip gradle.zip
RUN rm gradle.zip
RUN mv gradle-8.3 gradle

# Собираем JAR файл используя Gradle
RUN ./gradle/bin/gradle shadowJar --no-daemon

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
