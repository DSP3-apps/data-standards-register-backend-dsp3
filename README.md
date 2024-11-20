# openregister-java
A java implementation of a register

[![Build Status](https://travis-ci.org/openregister/openregister-java.svg?branch=master)](https://travis-ci.org/openregister/openregister-java)

## Build dependencies
[You need to build these 3 dependencies and add them to the lib folder](libs/README.md)
[dropwizard-logstash build_46](https://github.com/TPZDSP3/dropwizard-logstash)
[objecthash-java](https://github.com/TPZDSP3/objecthash-java)
[verifiable-log](https://github.com/TPZDSP3/verifiable-log)

## Running locally

You can spin up a local copy of `openregister-java` using Docker with the following:

    bash -c 'ENVIRONMENT=beta REGISTERS=country ./run-application.sh'

This will do the following in Docker containers:

- build the application from source.
- start and configure the DB.
- run the "basic" registers: register, field and datatype with data cloned from the specified phase (listens on `127.0.0.1:8081`).
- run a register configured with `config.docker.register.yaml` (listens on `127.0.0.1:8080`).

### Experimenting locally

If you want to view/change the basic registers you can do that by sending requests to `127.0.0.1:8081` with an appropriate `Host` header:

    curl 127.0.0.1:8081/records -H "Host: field"

If the basic registers change you can restart your register so that it sees the changes:

    docker restart openregister-register

See detailed steps for experimenting locally [here](RUNNING_LOCALLY.md).

### Running on Cloud Foundry/PaaS

- [Configure PaaS access](https://docs.cloud.service.gov.uk/#quick-setup-guide).
- Build the application (`./gradlew assemble`)
- Configure [a Postgres service](https://docs.cloud.service.gov.uk/#using-database-services).
- Tweak `manifest.yml` so it has appropriate values.
- Deploy (`cf push {name} -p deploy/openregister-java.jar`).

## Development

### Requirements

- Java 1.8+
- Postgres DB 9.5+ (and in particular `psql` and `createuser` need to be on your PATH)
- Python 3.6

### Build and Run project

- Install and run postgres db
- Checkout project
- Run the `./go` script to set everything up
- Build project using command `./gradlew clean build`

### IntelliJ setup
- `File > Open` your checkout directory
- `Run > Edit Configurations`
- Edit `RegisterRunner`
- Use classpath of module: `openregister-java_main`
- JRE: *select your installed JRE e.g.* `1.8`
- `Run > Run... > RegisterRunner`

To test if everything has worked:

`$ curl 127.0.0.1:8080/records -H "Host: school"`

should respond with:

 `Register undefined`

To load data into the register:

 `cat country-data.rsf | curl 127.0.0.1:8080/load-rsf -u foo:bar --data-binary @- -H "Host: country" -H "Content-Type: application/uk-gov-rsf"`

### Working on the frontend

[![Standard - JavaScript Style Guide](https://cdn.rawgit.com/feross/standard/master/badge.svg)](https://github.com/feross/standard)

### Updating the styles

Recompile the `.scss` files to `.css` via Gradle task:

    ./gradlew compileSass

### Update the govuk_frontend_toolkit and govuk_template to the latest version

    $ git submodule foreach git pull origin master
