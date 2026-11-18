FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S smarthealth && adduser -S smarthealth -G smarthealth

COPY target/prediction-service-*.jar /app/prediction-service.jar

USER smarthealth:smarthealth

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/prediction-service.jar"]
