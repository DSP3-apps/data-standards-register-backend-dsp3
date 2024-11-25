#!/usr/bin/env bash
set -e

ENVIRONMENT=${ENVIRONMENT:-beta}
REGISTERS=${REGISTERS:-"territory"}

case ${ENVIRONMENT} in
  discovery) DOMAIN="discovery.register.register-research.cloud";;
  alpha)     DOMAIN="alpha.register.register-research.cloud";;
  beta)      DOMAIN="register.register-research.cloud";;
  test)      DOMAIN="test.register.register-research.cloud";;
  live)      DOMAIN="register.register-research.cloud";;  
  *)         DOMAIN="$ENVIRONMENT.register.register-research.cloud";;
esac

function on_exit {
  #echo "Stopping and removing containers..."
  #docker-compose -p openregister-java-territory --file docker-compose.territory-register.yml down
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

#echo "Starting \"$ENVIRONMENT\" environment."
#echo "Starting territory register..."
#docker-compose -p openregister-java-territory --file docker-compose.territory-register.yml up -d
#wait_for_http_on_port 4012 openregister-java-territory

#for register in "register" "datatype" "field"; do
#  echo "Loading $register..."
#  curl \
#    --fail \
#    --header "Content-Type: application/uk-gov-rsf" \
#    --header "Host: $register" \
#    --data-binary @<(curl "https://$register.$DOMAIN/download-rsf") \
#    --user foo:bar \
#    "http://localhost:8081/load-rsf"
#done

for register in "territory"; do
  echo "Loading ${register}..."
  echo "Reading $PWD/${register}.rsf"
  curl \
    --fail \
    --header "Content-Type: application/uk-gov-rsf" \
    --header "Host: $register" \
    --data-binary "@$PWD/${register}.rsf" \
    --user foo:bar \
    "http://localhost:4012/load-rsf"
done

echo "Register register is ready on https://territory.register.register-research.cloud"

#do_nothing_forever
