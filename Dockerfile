FROM maven:3.6.1-jdk-11-slim
ARG VCS_REF

RUN mkdir /app
WORKDIR /app
ADD pom.xml pom.xml

ADD src src
RUN mvn clean install -DskipTests=true

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "target/bank-1.0-SNAPSHOT-jar-with-dependencies.jar"]