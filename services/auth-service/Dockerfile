FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S smarthealth && adduser -S smarthealth -G smarthealth

COPY target/auth-service-*.jar /app/auth-service.jar

USER smarthealth:smarthealth

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/auth-service.jar"]
