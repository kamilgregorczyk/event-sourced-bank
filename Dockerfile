FROM maven:3.6.0-jdk-8-alpine

RUN mkdir /app
WORKDIR /app
ADD pom.xml pom.xml
RUN mvn verify

ADD src src
RUN mvn clean package

EXPOSE 8000
RUN ls /app/target
CMD ["/bin/bash"]