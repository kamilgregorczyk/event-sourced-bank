[![pipeline status](https://gitlab.com/kamilgregorczyk/event-sourced-bank/badges/master/pipeline.svg)](https://gitlab.com/kamilgregorczyk/event-sourced-bank/pipelines)
[![qualitygate](https://sonarcloud.io/api/project_badges/measure?project=kamilgregorczyk_event-sourced-bank&metric=alert_status)](https://sonarcloud.io/dashboard?id=kamilgregorczyk_event-sourced-bank)
[![coverage](https://sonarcloud.io/api/project_badges/measure?project=kamilgregorczyk_event-sourced-bank&metric=coverage)](https://sonarcloud.io/dashboard?id=kamilgregorczyk_event-sourced-bank)
[![grade](https://sonarcloud.io/api/project_badges/measure?project=kamilgregorczyk_event-sourced-bank&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=kamilgregorczyk_event-sourced-bank)
[![](https://images.microbadger.com/badges/commit/uniqe15/event-sourced-bank.svg)](https://microbadger.com/images/uniqe15/event-sourced-bank "Get your own commit badge on microbadger.com")

POC of a bank which runs in-memory. It allows to create accounts, change names and transfer money between two accounts. Consistency between two models is  achieved with event sourcing (no CQRS so far) and bouncing events between two aggregates.

It requires no external dependencies as everything is in-memory (event bus, storage etc.)

There is no mechanism for rolling back (or continuing) transactions which failed because of a hardware error (service got restarted etc.) as everything is in-memory anyway. It could be easily implemented with a cron which would need to find all the unsucceeded and uncancelled transactions that were not modified in last 30 minutes and call CancelTransactionCommand.

## Endpoints
* [List Accounts](doc/listaccounts.md) : `GET /api/account/listAccounts`
* [Get Account](doc/getaccount.md) : `GET /api/account/getAccount/:uuid`
* [Change Full Name](doc/changefullname.md) : `POST /api/account/changefullname/:uuid`
* [Create Account](doc/createaccount.md) : `POST /api/account/createAccount`
* [Transfer Money](doc/transfermoney.md) : `POST /api/account/transfermoney`


## Links

* [GitLab Pipelines](https://gitlab.com/kamilgregorczyk/event-sourced-bank/pipelines)
* [SonarQube Dashboard](https://sonarcloud.io/dashboard?id=kamilgregorczyk_event-sourced-bank)
* [DockerHub](https://cloud.docker.com/u/uniqe15/repository/docker/uniqe15/event-sourced-bank)
