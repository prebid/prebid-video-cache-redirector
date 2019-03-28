FROM maven:3-jdk-8 as build

RUN mkdir -p /build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -e -C -T 1C org.apache.maven.plugins:maven-dependency-plugin:3.0.2:go-offline

COPY src ./src
RUN mvn package

FROM openjdk:8-alpine

ENV JAR_FILE prebid-video-cache-redirector.jar
ENV CONFIG_FILE config.json

EXPOSE 8080

COPY --from=build /build/target/$JAR_FILE $JAR_FILE
COPY src/main/config/$CONFIG_FILE $CONFIG_FILE

ENTRYPOINT java -jar $JAR_FILE -conf $CONFIG_FILE
