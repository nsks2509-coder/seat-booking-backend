#!/bin/sh
set -eu

case "${DATABASE_URL:-}" in
  postgres://*|postgresql://*)
    export JDBC_DATABASE_URL="jdbc:postgresql://${DATABASE_URL#*://}"
    ;;
esac

exec java -XX:MaxRAMPercentage=75 -jar /app/app.jar
