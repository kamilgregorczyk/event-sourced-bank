FROM maven:3.8.5-eclipse-temurin-11-alpine
ARG VCS_REF

RUN export MAVEN_OPTS="$MAVEN_OPTS -Djavax.net.debug=ssl"

RUN mkdir /app
WORKDIR /app
ADD pom.xml pom.xml
RUN mvn dependency:resolve

ADD src src
RUN mvn clean install -DskipTests=true

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "target/bank-1.0-SNAPSHOT-jar-with-dependencies.jar"]