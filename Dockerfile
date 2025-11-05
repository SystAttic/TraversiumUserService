FROM eclipse-temurin:17-jdk
MAINTAINER Traversium Developers
WORKDIR /opt/user-service

COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/opt/user-service/app.jar"]