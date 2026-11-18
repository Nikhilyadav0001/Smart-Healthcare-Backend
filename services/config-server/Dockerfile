FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S smarthealth && adduser -S smarthealth -G smarthealth

COPY target/config-server-*.jar /app/config-server.jar

USER smarthealth:smarthealth

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/config-server.jar"]
