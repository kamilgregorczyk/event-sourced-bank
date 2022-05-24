FROM eclipse-temurin:11-jdk
ARG VCS_REF

RUN mkdir /app
WORKDIR /app
ADD pom.xml pom.xml
ADD .mvn .mvn
ADD mvnw mvnw
ADD mvnw.cmd mvnw.cmd
RUN ./mvnw dependency:resolve

ADD src src
RUN ./mvnw clean install -DskipTests=true

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "target/bank-1.0-SNAPSHOT-jar-with-dependencies.jar"]