FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# Копируем файлы Gradle
COPY gradle gradle
COPY gradlew gradlew
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle

# Копируем исходный код
COPY src src

# Запускаем сборку Gradle
RUN ./gradlew clean build -x test

# Этап запуска приложения
FROM openjdk:21-jdk-slim

WORKDIR /app

# Копируем собранный JAR файл
COPY --from=builder /app/build/libs/KafkaApp-0.0.1-SNAPSHOT.jar app.jar

# Запускаем приложение
ENTRYPOINT ["java","-jar","app.jar"]
