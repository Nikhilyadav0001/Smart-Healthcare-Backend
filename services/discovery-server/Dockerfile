FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S smarthealth && adduser -S smarthealth -G smarthealth

COPY target/discovery-server-*.jar /app/discovery-server.jar

USER smarthealth:smarthealth

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "/app/discovery-server.jar"]
