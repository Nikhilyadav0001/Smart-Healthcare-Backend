FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S smarthealth && adduser -S smarthealth -G smarthealth

COPY target/health-data-service-*.jar /app/health-data-service.jar

USER smarthealth:smarthealth

EXPOSE 8083 9083

ENTRYPOINT ["java", "-jar", "/app/health-data-service.jar"]
