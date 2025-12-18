FROM gradle:8.5-jdk21 AS builder

WORKDIR /app
COPY . .

RUN gradle clean build --no-daemon

FROM openjdk:21-slim

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
