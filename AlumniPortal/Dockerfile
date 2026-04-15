# -------- Stage 1: Build --------
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy project files
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src src

# Give permission to mvnw
RUN chmod +x ./mvnw

# Build jar
RUN ./mvnw clean package -DskipTests

# -------- Stage 2: Run --------
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run app
ENTRYPOINT ["java","-jar","app.jar"]