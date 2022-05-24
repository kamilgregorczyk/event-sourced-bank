FROM eclipse-temurin:11-jdk-alpine
ARG VCS_REF

RUN mkdir /app
WORKDIR /app
COPY . .
RUN ./gradlew shadowJar

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "build/libs/bank-1.0-SNAPSHOT-all.jar"]