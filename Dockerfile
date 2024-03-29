FROM maven:3.6.3-jdk-14 AS build
COPY ./ /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package -Dmaven.test.skip=true

FROM openjdk:14-jdk-slim
COPY --from=build /usr/src/app/mcnative-actionframework-service-endpoint/target/mcnative-actionframework-service-endpoint.jar McNativeActionFrameworkServicEndpoint.jar

ENTRYPOINT ["java", "-jar", "McNativeActionFrameworkServicEndpoint.jar"]
