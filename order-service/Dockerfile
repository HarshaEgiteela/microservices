
# syntax=docker/dockerfile:1

# Stage 1: Build Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

# Cache Maven dependencies across Docker builds
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -ntp clean package -Dmaven.test.skip=true

# Stage 2: Run Spring Boot application
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
