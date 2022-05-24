FROM eclipse-temurin:11-jdk-alpine
ARG VCS_REF

RUN mkdir /app
WORKDIR /app
ADD pom.xml pom.xml
ADD mvnw mvnw
ADD .mvn .mvn

ADD src src
RUN ./mvnw clean install -DskipTests=true

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "target/bank-1.0-SNAPSHOT-jar-with-dependencies.jar"]