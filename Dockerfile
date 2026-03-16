FROM amazoncorretto:17-alpine
VOLUME /tmp
RUN mkdir -p /app/temp && chmod 777 /app/temp
COPY build/libs/team-c-back-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Xmx1408m", "-Xms512m", "-XX:MaxMetaspaceSize=256m", "-XX:+UseZGC", "-XX:ConcGCThreads=2", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/app/temp/heapdump.hprof", "-Djava.io.tmpdir=/app/temp", "-jar", "app.jar"]
