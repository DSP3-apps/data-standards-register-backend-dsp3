#!/usr/bin/env bash

######################################################################################
##                      load foobar data into registers                             ##
## 1. Add field definitions to the global field register where they are missing.    ##
## 2. Choose a primary key for your register. This will also be the name of         ##
##    your register. For example, foobar. Add the name and field definition to      ##
##    the global field register.                                                    ##
##    EXAMPLE [{"field": "foobar" "datatype": "string", "phase": "alpha",           ##
##              "cardinality": "1", "text": "test field"}]                          ##
## 3. Create an entry for the register in the register register.                    ##
##    EXAMPLE [{"phase":"alpha","registry":"government-digital-service",            ##
##              "text":"A test register","fields":["foobar","name","start-date",    ##
##              "end-date"],"register":"foobar"}]                                   ##
## 4. Check this is successfull at field.local.openregister.org:8081/records and    ##
##    register.local.openregister.org:8081/records. You should see the name of      ##
##    your register, such as foobar, as the first record.                           ##
## 5. Populate the new register with its register definition and field definitions. ##
## 6. You can now load the foobar data.                                             ##
######################################################################################

######################################################################################
#################################2021-10-27###########################################
######################################################################################
##                                                                                  ##
##                       load territory register definition                         ##
##                                                                                  ##
##    Note: apostrophe - use '"'"' to escape the apostrophe.                        ##
##          End of line termination must be unix LF                                 ##
##                                                                                  ##
##    register: "territory"                                                         ##
##                                                                                  ##
##    registry: "foreign-commonwealth-office"                                       ##
##                                                                                  ##
##    field definitions:                                                            ##
##    "territory",                                                                  ##
##    "name",                                                                       ##
##    "official-name",                                                              ##
##    "start-date",                                                                 ##
##    "end-date"                                                                    ##
##                                                                                  ##
##                        register register port 8081                               ##
##                          field register  port 4001                               ##
##                       datatype register  port 4008                               ##
##                        country register  port 4011                               ##
##                      territory register  port 4012                               ##
##                                                                                  ##
######################################################################################
##Ross Mackintosh#################2121-10-27##########################################
######################################################################################

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

echo "Entering definition in \"$ENVIRONMENT\" domain."
echo "Starting territory register..."
docker-compose -p openregister-java-territory --file docker-compose.territory-register.yml --compatibility up -d
wait_for_http_on_port 4012 openregister-java-territory

# 1. Add any register specific field definitions to the global field register. EXAMPLE: "official-name","citizen-names"
echo '[{"cardinality":"1","datatype":"string","field":"official-name","phase":"beta","text":"The official or technical name of a record."}]' | python3 ./scripts/json-to-rsf/json2rsf.py user field | curl http://localhost:4001/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar

# 2. Add the register name field specification to the global field register.
echo '[{"cardinality":"1","datatype":"string","field":"territory","phase":"beta","register":"territory","text":"The territory'"'"'s ISO 3166-1 alpha3 code. Unique codes have been created for territories that don'"'"'t have an existing ISO code."}]' | python3 ./scripts/json-to-rsf/json2rsf.py user field | curl http://localhost:4001/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar

# 3. Create an entry for the register in the global register register.
echo '[{"fields":["territory","name","official-name","start-date","end-date"],"phase":"beta","register":"territory","registry":"foreign-commonwealth-office","text":"British English-language names and descriptive terms for political, administrative and geographical entities that aren'"'"'t recognised as countries by the UK"}]' | python3 ./scripts/json-to-rsf/json2rsf.py user register | curl http://localhost:8081/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"fields":["territory","name","official-name","start-date","end-date"],"phase":"beta","register":"territory","registry":"foreign-commonwealth-office","text":"British English names and descriptive terms for political, administrative and geographical entities that are not recognised as countries by the UK government"}]' | python3 ./scripts/json-to-rsf/json2rsf.py user register | curl http://localhost:8081/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"fields":["territory","name","official-name","start-date","end-date"],"phase":"beta","register":"territory","registry":"foreign-commonwealth-office","text":"British English names of territories not currently recognised as countries by the UK government"}]' | python3 ./scripts/json-to-rsf/json2rsf.py user register | curl http://localhost:8081/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar


# 4. Check this is successful.

# 5. Populate the new register with its register definition and field definitions.
echo "[system][name]"
echo "[system][register-name]"
echo "[system][custodian]"
echo "[system][field:territory]"
echo "[system][field:name]"
echo "[system][field:official-name]"
echo "[system][field:start-date]"
echo "[system][field:end-date]"
echo "[system][register:territory]"

echo '[{"name":"territory"}]' | python3 ./scripts/json-to-rsf/json2rsf.py system name | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"register-name":"Territory register"}]' | python3 ./scripts/json-to-rsf/json2rsf.py system register-name | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"register-name":"Territory"}]' | python3 ./scripts/json-to-rsf/json2rsf.py system register-name | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"custodian":"David de Silva"}]' | python3 ./scripts/json-to-rsf/json2rsf.py system custodian | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"cardinality":"1","datatype":"string","field":"territory","phase":"beta","register":"territory","text":"The territory'"'"'s ISO 3166-1 alpha3 code. Unique codes have been created for territories that don'"'"'t have an existing ISO code."}]' | python3 ./scripts/json-to-rsf/json2rsf.py system field:territory | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"cardinality":"1","datatype":"string","field":"name","phase":"beta","text":"The commonly-used name of a record."}]' | python3 ./scripts/json-to-rsf/json2rsf.py system field:name | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"cardinality":"1","datatype":"string","field":"official-name","phase":"beta","text":"The official or technical name of a record."}]' | python3 ./scripts/json-to-rsf/json2rsf.py system field:official-name | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"cardinality":"1","datatype":"datetime","field":"start-date","phase":"beta","text":"The date a record first became relevant to a register. For example, the date a country was first recognised by the UK."}]' | python3 ./scripts/json-to-rsf/json2rsf.py system field:start-date | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"cardinality":"1","datatype":"datetime","field":"end-date","phase":"beta","text":"The date a record stopped being applicable. For example, the date a school closed down."}]' | python3 ./scripts/json-to-rsf/json2rsf.py system field:end-date | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"fields":["territory","name","official-name","start-date","end-date"],"phase":"beta","register":"territory","registry":"foreign-commonwealth-office","text":"British English-language names and descriptive terms for political, administrative and geographical entities that aren'"'"'t recognised as countries by the UK"}]' | python3 ./scripts/json-to-rsf/json2rsf.py system register:territory | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"fields":["territory","name","official-name","start-date","end-date"],"phase":"beta","register":"territory","registry":"foreign-commonwealth-office","text":"British English names and descriptive terms for political, administrative and geographical entities that are not recognised as countries by the UK government"}]' | python3 ./scripts/json-to-rsf/json2rsf.py system register:territory | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar
echo '[{"fields":["territory","name","official-name","start-date","end-date"],"phase":"beta","register":"territory","registry":"foreign-commonwealth-office","text":"British English names of territories not currently recognised as countries by the UK government"}]' | python3 ./scripts/json-to-rsf/json2rsf.py system register:territory | curl http://localhost:4012/load-rsf -H "Content-Type: application/uk-gov-rsf" --data-binary @- -u foo:bar


# 5. Populate the new register with its register definition and field definitions.
# cat territory.rsf | curl 127.0.0.1:4012/load-rsf -u foo:bar --data-binary @- -H "Host: territory" -H "Content-Type: application/uk-gov-rsf"
