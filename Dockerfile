# [Stage 1] 빌드 단계
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
# 멀티 스테이지(Multi-stage)

# [Stage 2] 실행 단계
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /server
COPY --from=builder /build/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
