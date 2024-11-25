#!/usr/bin/env bash
set -e

ENVIRONMENT=${ENVIRONMENT:-beta}
REGISTERS=${REGISTERS:-"country"}

case ${ENVIRONMENT} in
  discovery) DOMAIN="discovery.register.register-research.cloud";;
  alpha)     DOMAIN="alpha.register.register-research.cloud";;
  beta)      DOMAIN="beta.register.register-research.cloud";;
  test)      DOMAIN="test.register.register-research.cloud";;
  live)      DOMAIN="register.register-research.cloud";;
  *)         DOMAIN="$ENVIRONMENT.register.register-research.cloud";;
esac

function on_exit {
  echo "Stopping and removing containers..."
  docker-compose -p openregister-java-field --file docker-compose.field.yml down
  docker-compose -p openregister-java-datatype --file docker-compose.datatype.yml down
  docker-compose -p openregister-java --file docker-compose.basic.yml down
  docker network prune --force
  docker volume prune --force
  exit
}

function wait_for_http_on_port {
  while ! curl "http://localhost:$1" --silent --fail --output /dev/null;
  do
    if [ $(docker inspect -f {{.State.Running}} $2) != 'true' ]; then
      echo "Container $2 unexpectedly stopped while waiting for it to open port $1"
      exit 1
    fi
    echo "Waiting for HTTP on :$1"
    sleep 1
  done
}

function do_nothing_forever {
  tail -f /dev/null
}

trap on_exit EXIT

#git submodule update --init

#if [ ! -e "./deploy/openregister-java.jar" ]
#then
#  docker run \
#    --rm \
#    --volume "$PWD":/usr/src/openregister-java \
#    --workdir /usr/src/openregister-java \
#    openjdk:8 \
#      bash -c "./gradlew assemble"
#fi

if [ ! -e ".env" ]; then
    cat > .env <<EOF

# Should Docker restart your containers if they go down in unexpected ways?
#export DOCKER_RESTART_POLICY=unless-stopped
export DOCKER_RESTART_POLICY=no

# What healthcheck test command do you want to run? In development, having it
# curl your web server will result in a lot of log spam, so setting it to
# /bin/true is an easy way to make the healthcheck do basically nothing.
#export DOCKER_WEB_HEALTHCHECK_TEST=curl localhost:8000/up
export DOCKER_WEB_HEALTHCHECK_TEST=/bin/true

# Environment settings for Docker container postgreSQL database.
PG_WORK_MEM=${PG_WORK_MEM:-16MB}
PG_MAINTENANCE_WORK_MEM=${PG_MAINTENANCE_WORK_MEM:-256MB}
EOF
chmod a+rw .env
fi

echo "Starting \"$ENVIRONMENT\" environment."
echo "Starting basic registers..."
# docker-compose -p openregister-java --file docker-compose.basic.yml --compatibility up -d
wait_for_http_on_port 8081 openregister-basic

for register in "register" "datatype" "field"; do
  echo "Loading ${register}"
  echo "Reading $PWD/rsf-${register}-2021-12-01_v2a.rsf"
  curl \
    --fail \
    --header "Content-Type: application/uk-gov-rsf" \
    --header "Host: $register" \
    --data-binary "@$PWD/rsf-${register}-2021-12-01_v2a.rsf" \
    --user foo:bar \
    "http://localhost:8081/load-rsf"
done
echo "Register register is ready on https://register.register.register-research.cloud"

#echo "Starting environment based off \"$ENVIRONMENT\""
echo "Starting basic registers field..."
docker-compose -p openregister-java-field --file docker-compose.field.yml --compatibility up -d
wait_for_http_on_port 4001 openregister-field

echo "Field register is ready on https://field.register.register-research.cloud"

#echo "Starting environment based off \"$ENVIRONMENT\""
echo "Starting basic registers datatype..."
docker-compose -p openregister-java-datatype --file docker-compose.datatype.yml --compatibility up -d
wait_for_http_on_port 4008 openregister-datatype

echo "Datatype register is ready on https://datatype.register.register-research.cloud"

do_nothing_forever
