FROM amazoncorretto:17-alpine
VOLUME /tmp
RUN mkdir -p /app/temp && chmod 777 /app/temp
COPY build/libs/team-c-back-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Djava.io.tmpdir=/app/temp", "-jar", "app.jar"]
