#
# Build stage
#
FROM maven:3.9.12-eclipse-temurin-17-alpine AS build
COPY pom.xml /home/app/pom.xml
WORKDIR /home/app
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

#
# Package stage
#
FROM eclipse-temurin:17-jre-alpine
ARG JAR_FILE=/home/app/target/*.jar
COPY --from=build $JAR_FILE /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]