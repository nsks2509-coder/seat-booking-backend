#!/bin/sh
set -eu

case "${DATABASE_URL:-}" in
  postgres://*|postgresql://*)
    url="${DATABASE_URL#*://}"
    credentials="${url%%@*}"
    host_and_database="${url#*@}"
    database_host="${host_and_database%%/*}"
    database_name="${host_and_database#*/}"
    database_name="${database_name%%\?*}"
    export DATABASE_USERNAME="${credentials%%:*}"
    export DATABASE_PASSWORD="${credentials#*:}"
    export JDBC_DATABASE_URL="jdbc:postgresql://${database_host}/${database_name}"
    ;;
esac

exec java -XX:MaxRAMPercentage=75 -jar /app/app.jar
