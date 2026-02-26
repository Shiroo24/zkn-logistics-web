# Tahap Build
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY . .
# Paksa Maven menggunakan setting sederhana
RUN mvn clean package -DskipTests -Dmaven.main.skip=false

# Tahap Runtime
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]