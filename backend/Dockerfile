# Build stage: compile and package the backend (tests run in CI, not here).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# Runtime stage: only the JRE and the application jar.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home hipereats
COPY --from=build /app/target/*.jar app.jar
USER hipereats
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
