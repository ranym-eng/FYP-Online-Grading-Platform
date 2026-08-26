#!/bin/sh
set -eu

mkdir -p /app/generated
chown -R spring:spring /app/generated

exec su-exec spring java -jar /app/app.jar
