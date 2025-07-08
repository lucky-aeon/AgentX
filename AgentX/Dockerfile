FROM openjdk:8-jdk-alpine

WORKDIR /app

COPY target/agent-x-*.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]
