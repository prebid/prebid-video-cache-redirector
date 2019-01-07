FROM openjdk:8-alpine

ENV JAR_FILE prebid-video-cache-redirector.jar
ENV CONFIG_FILE config.json

EXPOSE 8080

COPY target/$JAR_FILE $JAR_FILE
COPY src/main/config/$CONFIG_FILE $CONFIG_FILE

ENTRYPOINT java -jar $JAR_FILE -conf $CONFIG_FILE
