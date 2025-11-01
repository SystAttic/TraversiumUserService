FROM openjdk:17-jdk-slim
MAINTAINER Traversium Developers
WORKDIR /opt/user-service

COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/opt/user-service/app.jar"]
CMD ["./start.sh"]