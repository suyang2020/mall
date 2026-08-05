FROM eclipse-temurin:17-jre
LABEL maintainer="mall"

WORKDIR /app
COPY target/mall-search-1.0-SNAPSHOT.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "/app/app.jar"]
