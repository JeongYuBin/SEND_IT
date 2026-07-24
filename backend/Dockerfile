FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S sendit && adduser -S sendit -G sendit
WORKDIR /app
COPY --from=build /app/target/sendit-api-*.jar app.jar
USER sendit
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

