#!/bin/sh
docker build --build-arg VCS_REF=$(git rev-parse --short HEAD) -t uniqe15/event-sourced-bank .