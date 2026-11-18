FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S smarthealth && adduser -S smarthealth -G smarthealth

COPY target/admin-server-*.jar /app/admin-server.jar

USER smarthealth:smarthealth

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/admin-server.jar"]
