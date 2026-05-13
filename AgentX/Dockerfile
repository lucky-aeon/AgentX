FROM eclipse-temurin:8-jdk

WORKDIR /app

COPY target/agent-x-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
