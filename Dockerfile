FROM openjdk:17-jdk-slim
MAINTAINER Traversium Developers
WORKDIR /opt/user-service
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/opt/user-service/app.jar"]
