FROM eclipse-temurin:21

COPY target/rehearser.jar /
ENTRYPOINT ["java", "-jar", "rehearser.jar", "serve"]
