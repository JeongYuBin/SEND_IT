FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache ffmpeg nodejs python3 py3-pip \
    && python3 -m venv /opt/yt-dlp \
    && /opt/yt-dlp/bin/pip install --no-cache-dir yt-dlp==2026.6.9 \
    && addgroup -S sendit && adduser -S sendit -G sendit \
    && mkdir -p /app/data/uploads \
    && chown -R sendit:sendit /app/data
WORKDIR /app
COPY --from=build /app/target/sendit-api-*.jar app.jar
ENV MEDIA_DOWNLOADER_EXECUTABLE=/opt/yt-dlp/bin/yt-dlp
USER sendit
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
