# Gunakan JDK yang ringan
FROM eclipse-temurin:17-jdk-alpine
# Copy file jar hasil build ke dalam container
COPY target/*.jar app.jar
# Batasi RAM agar tidak melebihi kuota gratis Render
ENTRYPOINT ["java","-Xmx300m","-Xss512k","-jar","/app.jar"]