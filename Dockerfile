FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY . .

# Устанавливаем только необходимые пакеты
RUN apk add --no-cache bash

# Используем Gradle wrapper если есть, или скачиваем
RUN if [ -f "./gradlew" ]; then ./gradlew shadowJar; else \
    wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip && \
    unzip -q gradle-8.5-bin.zip && \
    rm gradle-8.5-bin.zip && \
    ./gradle-8.5/bin/gradle shadowJar; \
    fi

EXPOSE 8080
CMD ["java", "-jar", "build/libs/messenger-server.jar"]
