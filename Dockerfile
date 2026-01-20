# Stage 1: Build the JAR
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR (WITH FONTS FIX)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 1. INSTALL FONTS (Crucial for Apache POI)
RUN apk add --no-cache fontconfig ttf-dejavu

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]