# Gunakan image Maven dengan Java 17 yang lebih kompatibel
FROM maven:3.8.4-openjdk-17 AS build
WORKDIR /app

# Copy seluruh isi folder ZKNLOG ke dalam image
COPY . .

# Jalankan build dan lewati pengetesan agar cepat
RUN mvn clean package -DskipTests

# Tahap akhir untuk menjalankan aplikasi
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Port standar Spring Boot
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]