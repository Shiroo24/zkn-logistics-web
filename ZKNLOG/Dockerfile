# Gunakan image Java yang ringan
FROM eclipse-temurin:17-jdk-alpine
# Copy file jar hasil build ke dalam container
COPY target/*.jar app.jar
# Jalankan aplikasinya
ENTRYPOINT ["java","-jar","/app.jar"]