# Stage 1: Build the JAR
# We keep Alpine for building because it is fast
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR (THE FIX)
# We switch to Standard Linux (Debian/Ubuntu) for the running app.
# This solves the Font/Graphics issues permanently.
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install basic font configuration just to be 100% safe
RUN apt-get update && apt-get install -y fontconfig libfreetype6 && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]