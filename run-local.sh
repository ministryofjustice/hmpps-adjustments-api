#
# This script is used to run the Adjustment API locally, to interact with
# existing Postgresql and hmpps-auth containers.
#
# The environment variables here will also override values supplied in spring profile properties, specifically
# around removing the SSL connection to the database and setting the DB properties, SERVER_PORT and client credentials
# to match those used in the docker-compose files.
#

# Server port - avoid clash with prison-api
export SERVER_PORT=8089

# Provide the DB connection details to local container-hosted Postgresql DB
# Match with the credentials set in docker-compose.yml
export DB_SERVER=localhost
export DB_NAME=adjustments
export DB_USER=adjustments
export DB_PASS=adjustments
export SYSTEM_CLIENT_ID=XXX_ENTER_SYSTEM_CLIENT_ID
export SYSTEM_CLIENT_SECRET="XXX_ENTER_SYSTEM_CLIENT_SECRET"

# Provide URLs to other local container-based dependent services
# Match with ports defined in docker-compose.yml
# export HMPPS_AUTH_URL=http://localhost:9090/auth
export HMPPS_AUTH_URL=https://sign-in-dev.hmpps.service.justice.gov.uk/auth
export PRISON_API_URL=https://api-dev.prison.service.justice.gov.uk

# Make the connection without specifying the sslmode=verify-full requirement
export SPRING_DATASOURCE_URL='jdbc:postgresql://${DB_SERVER}/${DB_NAME}'

# Run the application with stdout and dev profiles active
SPRING_PROFILES_ACTIVE=stdout,dev,localstack ./gradlew bootRun

# End
