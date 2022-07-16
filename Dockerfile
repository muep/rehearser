FROM openjdk:17

COPY target/rehearser.jar /
ENTRYPOINT ["java", "-jar", "rehearser.jar", "serve"]
