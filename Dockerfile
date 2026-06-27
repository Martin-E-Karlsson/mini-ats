# ---- Build stage: compile and package with the Maven wrapper ----
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

# ---- Run stage: small JRE image running the fat jar ----
FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Keep the JVM within Render's free-tier memory (512MB).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
