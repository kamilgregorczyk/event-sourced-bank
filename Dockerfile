FROM maven:3.6.0-jdk-8-alpine
ARG VCS_REF

RUN mkdir /app
WORKDIR /app
ADD pom.xml pom.xml
RUN mvn verify

ADD src src
RUN mvn package

LABEL org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/kamilgregorczyk/event-sourced-bank"

EXPOSE 8000
RUN ls /app/target
ENTRYPOINT ["java", "-jar", "target/bank-1.0-SNAPSHOT-jar-with-dependencies.jar"]