FROM eclipse-temurin:17-jre
LABEL maintainer="mall"

WORKDIR /app
COPY target/mall-admin-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "/app/app.jar"]
