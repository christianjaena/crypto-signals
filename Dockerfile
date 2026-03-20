# Stage 1: Build the JAR
FROM eclipse-temurin:25 AS build
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven

# Copy all source files
COPY . .

# Build the project
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:25-jdk
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Run the app
ENTRYPOINT ["java","-Xmx256m","-Xms128m","-jar","/app/app.jar"]