FROM openjdk:17-alpine
COPY build/libs/team-c-back-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
