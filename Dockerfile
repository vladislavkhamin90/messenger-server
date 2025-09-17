FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY . .

# Устанавливаем необходимые пакеты
RUN apk add --no-cache wget unzip

# Скачиваем и устанавливаем Gradle
RUN wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip && \
    unzip -q gradle-8.5-bin.zip && \
    rm gradle-8.5-bin.zip

# Даем права на выполнение
RUN chmod +x ./gradle-8.5/bin/gradle

# Копируем зависимости сначала для кэширования
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradle-8.5/bin/gradle dependencies --no-daemon

# Копируем исходный код и собираем
COPY src ./src
RUN ./gradle-8.5/bin/gradle shadowJar --no-daemon --stacktrace

# Запускаем сервер
EXPOSE 8080
CMD ["java", "-jar", "build/libs/messenger-server.jar"]
