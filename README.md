[![pipeline status](https://gitlab.com/kamilgregorczyk/event-sourced-bank/badges/master/pipeline.svg)](https://gitlab.com/kamilgregorczyk/event-sourced-bank/pipelines)
[![qualitygate](https://sonarcloud.io/api/project_badges/measure?project=kamilgregorczyk_event-sourced-bank&metric=alert_status)](https://sonarcloud.io/dashboard?id=kamilgregorczyk_event-sourced-bank)
[![coverage](https://sonarcloud.io/api/project_badges/measure?project=kamilgregorczyk_event-sourced-bank&metric=coverage)](https://sonarcloud.io/dashboard?id=kamilgregorczyk_event-sourced-bank)
[![grade](https://sonarcloud.io/api/project_badges/measure?project=kamilgregorczyk_event-sourced-bank&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=kamilgregorczyk_event-sourced-bank)
[![Known Vulnerabilities](https://snyk.io/test/github/kamilgregorczyk/event-sourced-bank/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/kamilgregorczyk/event-sourced-bank?targetFile=pom.xml)

POC of a bank which runs in-memory. It allows to create accounts, change names and transfer money between two accounts.
Consistency between two models is achieved with event sourcing (no CQRS so far) and bouncing events between two
aggregates in a sync way.

It requires no external dependencies as everything is in-memory (event bus, storage etc.)

Rolling back of unfinished transaction is implemented with a cron that finds unfinished transactions which were not
modified within last 30 minutes and cancels them.

## Endpoints

### Metric Endpoints

| Endpoint                           | Method | URL                            |
|------------------------------------|--------|--------------------------------|
| [Health Check](doc/healthcheck.md) | `GET`  | https://bank.exposee.app/      |

### Account Endpoints

| Endpoint                                  | Method | URL                                                        |
|-------------------------------------------|--------|------------------------------------------------------------|
| [List Accounts](doc/listaccounts.md)      | `GET`  | https://bank.exposee.app/api/account                       |
| [Get Account](doc/getaccount.md)          | `GET`  | https://bank.exposee.app/api/account/:UUID                 |
| [Change Full Name](doc/changefullname.md) | `PUT`  | https://bank.exposee.app/api/account/:UUID/changeFullName  |
| [Create Account](doc/createaccount.md)    | `POST` | https://bank.exposee.app/api/account                       |
| [Transfer Money](doc/transfermoney.md)    | `POST` | https://bank.exposee.app/api/account/transferMoney         |

## Links

* [GitLab Pipelines](https://gitlab.com/kamilgregorczyk/event-sourced-bank/pipelines)
* [SonarQube Dashboard](https://sonarcloud.io/dashboard?id=kamilgregorczyk_event-sourced-bank)
* [DockerHub](https://cloud.docker.com/u/uniqe15/repository/docker/uniqe15/event-sourced-bank)
