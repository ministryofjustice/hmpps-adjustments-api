# hmpps-adjustments-api
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-adjustments-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-adjustments-api "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-adjustments-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-adjustments-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-adjustments-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-adjustments-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://hmpps-adjustments-api-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)

This is an api for release date adjustments data.

# Running the service locally using run-local.sh
## Start the postgres database and auth
This will run the service locally. It starts the database and auth using docker compose then runs manage-offences-api via a bash script.
Run the following commands from the root directory of the project:
1. `docker-compose -f docker-compose-test.yml pull`
2. `docker-compose -f docker-compose-test.yml up --no-start`
3. `docker-compose -f docker-compose-test.yml start hmpps-auth adjustments-db`
4. `./run-local.sh`