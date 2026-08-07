FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM alpine:3.22 AS whisper-build
ARG WHISPER_CPP_VERSION=v1.7.6
RUN apk add --no-cache git cmake make g++ curl
RUN git clone --depth 1 --branch ${WHISPER_CPP_VERSION} https://github.com/ggerganov/whisper.cpp.git /whisper.cpp \
    && cmake -S /whisper.cpp -B /whisper.cpp/build \
        -DBUILD_SHARED_LIBS=OFF -DWHISPER_BUILD_TESTS=OFF -DWHISPER_BUILD_EXAMPLES=ON \
    && cmake --build /whisper.cpp/build --config Release -j 2 \
    && curl -L --fail --retry 3 \
        https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin \
        -o /whisper.cpp/models/ggml-base.bin

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache ffmpeg nodejs python3 py3-pip \
        tesseract-ocr tesseract-ocr-data-eng tesseract-ocr-data-kor \
        tesseract-ocr-data-chi_tra \
    && python3 -m venv /opt/yt-dlp \
    && /opt/yt-dlp/bin/pip install --no-cache-dir yt-dlp==2026.6.9 \
    && addgroup -S sendit && adduser -S sendit -G sendit \
    && mkdir -p /app/data/uploads \
    && chown -R sendit:sendit /app/data
WORKDIR /app
COPY --from=build /app/target/sendit-api-*.jar app.jar
COPY --from=whisper-build /whisper.cpp/build/bin/whisper-cli /usr/local/bin/whisper-cli
COPY --from=whisper-build /whisper.cpp/models/ggml-base.bin /opt/whisper/models/ggml-base.bin
ENV MEDIA_DOWNLOADER_EXECUTABLE=/opt/yt-dlp/bin/yt-dlp
USER sendit
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
