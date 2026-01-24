# Stage 1: Build the JAR
# We keep Alpine for building because it is fast
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the JAR
FROM eclipse-temurin:21-jre
WORKDIR /app

# --- THE FIX IS HERE ---
# Previously we only installed the engine (fontconfig). 
# Now we install the actual fonts (fonts-dejavu) so Java can write text.
RUN apt-get update && apt-get install -y fontconfig libfreetype6 fonts-dejavu && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]