FROM adoptopenjdk/openjdk11:jre-11.0.8_10-alpine
COPY target/*.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]