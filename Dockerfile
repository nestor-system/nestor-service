FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY target/*.jar NESTORService-1.2.0.jar
ENTRYPOINT ["java","-Xmx4096M", "-jar","/NESTORService-1.2.0.jar"]