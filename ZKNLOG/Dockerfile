# Gunakan Java 17 yang sudah termasuk Maven agar stabil
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy seluruh isi folder ZKNLOG
COPY . .

# Build aplikasi menjadi file .jar
RUN mvn clean package -DskipTests

# Jalankan aplikasi menggunakan Java
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]