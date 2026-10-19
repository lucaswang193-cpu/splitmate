# 第一階段：使用 Java 21 編譯並打包。
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/main/ src/main/

RUN bash ./mvnw -B package -DskipTests

# 第二階段：只保留執行環境和打包好的 JAR。
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]