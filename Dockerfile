# Build stage
FROM eclipse-temurin:17-jdk-alpine as builder
WORKDIR /app
COPY . .
RUN apk add --no-cache wget unzip && \
    wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip && \
    unzip -q gradle-8.5-bin.zip && \
    rm gradle-8.5-bin.zip && \
    ./gradle-8.5/bin/gradle build -x test --no-daemon --no-build-cache

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/messenger-server.jar .
EXPOSE 8080
CMD ["java", "-jar", "messenger-server.jar"]
