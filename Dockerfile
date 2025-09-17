# Build stage
FROM gradle:8.5-jdk17-alpine as builder
WORKDIR /app
COPY . .
RUN gradle fatJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/messenger-server.jar .
EXPOSE 8080
CMD ["java", "-jar", "messenger-server.jar"]
