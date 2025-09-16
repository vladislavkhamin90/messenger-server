FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY . .

# Устанавливаем Gradle
RUN apk add --no-cache wget unzip
RUN wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip
RUN unzip -q gradle-8.5-bin.zip
RUN rm gradle-8.5-bin.zip

# Собираем проект
RUN ./gradle-8.5/bin/gradle shadowJar --no-daemon

# Запускаем сервер
EXPOSE 8080
CMD ["java", "-jar", "build/libs/messenger-server.jar"]
