FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

RUN addgroup -S smarthealth && adduser -S smarthealth -G smarthealth

COPY target/user-profile-service-*.jar /app/user-profile-service.jar

USER smarthealth:smarthealth

EXPOSE 8082 9082

ENTRYPOINT ["java", "-jar", "/app/user-profile-service.jar"]
