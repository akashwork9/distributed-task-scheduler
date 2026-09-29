# Multi-stage Dockerfile for DTS Spring Boot Backend
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime Image
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="DTS Engineering Team"
LABEL description="Distributed Task Scheduler Core Backend"

WORKDIR /app

RUN addgroup -S dtsgroup && adduser -S dtsuser -G dtsgroup

COPY --from=builder /build/target/*.jar app.jar
RUN chown -R dtsuser:dtsgroup /app

USER dtsuser

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
