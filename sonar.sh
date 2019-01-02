#!/bin/sh
mvn sonar:sonar \
  -Dsonar.projectKey=kamilgregorczyk_event-sourced-bank \
  -Dsonar.organization=kamilgregorczyk-github \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.junit.reportPaths=target/surfire-reports \
  -Dsonar.jacoco.reportPaths=target/jacoco.exec \
  -Dsonar.login=${SONAR_QUBE}
