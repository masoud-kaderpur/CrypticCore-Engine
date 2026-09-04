FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -Dcheckstyle.skip=true

FROM eclipse-temurin:21.0.6_7-jre-alpine@sha256:d5c3bf1712a7bf32185c9ca62df5a3c6be4cfc8be37e7a8e8062fa9cfaea7bc6
WORKDIR /app

COPY --from=builder /app/target/CrypticCore-jar-with-dependencies.jar engine.jar

RUN addgroup -g 1000 -S enginegroup && \
    adduser -u 1000 -S engineuser -G enginegroup

USER engineuser

ENTRYPOINT ["java", "-jar", "engine.jar"]

ENV LOG_FORMAT=JSON