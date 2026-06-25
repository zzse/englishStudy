# 1단계: 빌드 환경 세팅 (Gradle 빌드)
FROM gradle:7.6-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build -x test --no-daemon

# 2단계: 실행 환경 세팅 (🚨 원인이던 openjdk 대신 표준 eclipse-temurin 이미지로 교체)
FROM eclipse-temurin:17-jre-jammy
EXPOSE 8080
COPY --from=build /home/gradle/src/build/libs/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]