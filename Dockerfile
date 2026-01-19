# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn -B clean package -DskipTests

# Stage 2: Create a minimal runtime image
FROM openjdk:17-jre-slim
WORKDIR /app
COPY --from=build /app/target/whisky-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]