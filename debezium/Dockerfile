FROM openjdk:21-jdk-slim AS builder

WORKDIR /app

# �������� ����� Gradle
COPY gradle gradle
COPY gradlew gradlew
COPY settings.gradle settings.gradle
COPY build.gradle build.gradle

# �������� �������� ���
COPY src src

# ��������� ������ Gradle
RUN ./gradlew clean build -x test

# ���� ������� ����������
FROM openjdk:21-jdk-slim

WORKDIR /app

# �������� ��������� JAR ����
COPY --from=builder /app/build/libs/KafkaApp-0.0.1-SNAPSHOT.jar app.jar

# ��������� ����������
ENTRYPOINT ["java","-jar","app.jar"]
