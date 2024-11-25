#!/usr/bin/env bash

######################################################################################
#################################2021-10-16###########################################
######################################################################################
##                                                                                  ##
##                      load country data into registers                            ##
##                                                                                  ##
##    Note: apostrophe - use '"'"' to escape the apostrophe.                        ##
##          End of line termination must be unix LF                                 ##
##                                                                                  ##
##    register: "country"                                                           ##
##                                                                                  ##
##    registry: "foreign-commonwealth-office"                                       ##
##                                                                                  ##
##    field definitions:                                                            ##
##    "country",                                                                    ##
##    "name",                                                                       ##
##    "official-name",                                                              ##
##    "citizen-names",                                                              ##
##    "start-date",                                                                 ##
##    "end-date"                                                                    ##
##                                                                                  ##
##                        register register port 8081                               ##
##                          field register  port 4001                               ##
##                       datatype register  port 4008                               ##
##                        country register  port 4011                               ##
##                                                                                  ##
######################################################################################
##Ross Mackintosh#################2121-10-16##########################################
######################################################################################

set -e

ENVIRONMENT=${ENVIRONMENT:-beta}
REGISTERS=${REGISTERS:-"country"}

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
  #docker-compose -p openregister-java-country --file docker-compose.country-register.yml down
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
#echo "Starting country register..."
#docker-compose -p openregister-java-country --file docker-compose.country-register.yml up -d
#wait_for_http_on_port 4011 openregister-java-country

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

for register in "country"; do
  echo "Loading ${register}"
  echo "Reading $PWD/${register}.rsf"
  curl \
    --fail \
    --header "Content-Type: application/uk-gov-rsf" \
    --header "Host: $register" \
    --data-binary "@$PWD/${register}.rsf" \
    --user foo:bar \
    "http://192.168.1.99:4011/load-rsf"
done

echo "Country register is ready on https://country.register.register-research.cloud"

#do_nothing_forever
