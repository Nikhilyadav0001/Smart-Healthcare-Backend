FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S smarthealth && adduser -S smarthealth -G smarthealth

COPY target/api-gateway-*.jar /app/api-gateway.jar

USER smarthealth:smarthealth

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/api-gateway.jar"]
