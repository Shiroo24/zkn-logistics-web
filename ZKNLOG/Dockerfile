# Tahap 1: Build
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
# Copy hanya file pom.xml dulu untuk memanfaatkan cache Docker
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy sisa kode dan build
COPY . .
RUN mvn clean package -DskipTests

# Tahap 2: Runtime
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]