FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY smart-invent-spring-backend/pom.xml smart-invent-spring-backend/pom.xml
RUN mvn -f smart-invent-spring-backend/pom.xml -DskipTests dependency:go-offline

COPY smart-invent-spring-backend smart-invent-spring-backend
RUN mvn -f smart-invent-spring-backend/pom.xml -DskipTests clean package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/smart-invent-spring-backend/target/smart-invent-spring-backend-1.0.0.jar app.jar

ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
