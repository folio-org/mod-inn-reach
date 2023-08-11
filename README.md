# mod-inn-reach

Copyright (C) 2021 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

<!-- ../../okapi/doc/md2toc -l 2 -h 4 README.md -->
* [Introduction](#introduction)
* [Compiling](#compiling)
* [Docker](#docker)
* [Installing the module](#installing-the-module)
* [Deploying the module](#deploying-the-module)
* [Additional information](#additional-information)

## Introduction

The module provides an access to INN-Reach.

### Environment variables:

| Name                          |       Default value       | Description                                                                                                                                                |
|:------------------------------|:-------------------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| JAVA_OPTIONS                  | -XX:MaxRAMPercentage=85.0 | Java options                                                                                                                                               |
| DB_HOST                       |         postgres          | Postgres hostname                                                                                                                                          |
| DB_PORT                       |           5432            | Postgres port                                                                                                                                              |
| DB_USERNAME                   |        folio_admin        | Postgres username                                                                                                                                          |
| DB_PASSWORD                   |             -             | Postgres password                                                                                                                                          |
| DB_DATABASE                   |       okapi_modules       | Postgres database name                                                                                                                                     |
| DB_QUERYTIMEOUT               |           60000           | Database query timeout                                                                                                                                     |
| DB_CHARSET                    |           UTF-8           | Database charset                                                                                                                                           |
| DB_MAXPOOLSIZE                |             5             | Database max pool size                                                                                                                                     |
| OKAPI_URL                     |             -             | OKAPI URL used to login system user, required                                                                                                              |
| ENV                           |           folio           | Logical name of the deployment, must be set if Kafka/Elasticsearch are shared for environments, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed     |
| SYSTEM_USER_PASSWORD          |             -             | Internal user password                                                                                                                                     |
| KAFKA_HOST                    |           kafka           | Kafka broker hostname                                                                                                                                      |
| KAFKA_PORT                    |           9092            | Kafka broker port                                                                                                                                          |
| KAFKA_SECURITY_PROTOCOL       |         PLAINTEXT         | Kafka security protocol used to communicate with brokers (SSL or PLAINTEXT)                                                                                |
| KAFKA_SSL_KEYSTORE_LOCATION   |             -             | The location of the Kafka key store file. This is optional for client and can be used for two-way authentication for client.                               |
| KAFKA_SSL_KEYSTORE_PASSWORD   |             -             | The store password for the Kafka key store file. This is optional for client and only needed if 'ssl.keystore.location' is configured.                     |
| KAFKA_SSL_TRUSTSTORE_LOCATION |             -             | The location of the Kafka trust store file.                                                                                                                |
| KAFKA_SSL_TRUSTSTORE_PASSWORD |             -             | The password for the Kafka trust store file. If a password is not set, trust store file configured will still be used, but integrity checking is disabled. |
| LOG_HTTP                      |           false           | Enable logging of all requests and responses.                                                                                                              |
| DEFAULT_INTERVAL              |           95000           | Default interval in ms ( preffred not to increase more than this value )                                                                                   |
 | MAX_FAILURE                   |            360            | Default max attempts                                                                                                                                       |
| DEFAULT_OFFSET                |          latest           | Default kafka offset                                                                                                                                       |
 | DEFAULT_CONCURRENCY           |      2                    | Deafult concurrency of kafka consumer                                                                                                                      |
| INNREACH_TENANTS              |             -             | This is a regex where list of tenants needs to be mentioned with pipe symbol as delimiter so that mod-inn-reach will listen only these tenant related topics. See here [Example value for INNREACH_TENANTS](#example-value-for-innreachtenants) |

## Compiling

```
   mvn install
```

See that it says "BUILD SUCCESS" near the end.

## Docker

Build the docker container with:

```
   docker build -t mod-inn-reach .
```

Test that it runs with:

```
   docker run -t -i -p 8081:8081 mod-inn-reach
```

## Installing the module

Follow the guide of
[Deploying Modules](https://github.com/folio-org/okapi/blob/master/doc/guide.md#example-1-deploying-and-using-a-simple-module)
sections of the Okapi Guide and Reference, which describe the process in detail.

First of all you need a running Okapi instance.
(Note that [specifying](../README.md#setting-things-up) an explicit 'okapiurl' might be needed.)

```
   cd .../okapi
   java -jar okapi-core/target/okapi-core-fat.jar dev
```

We need to declare the module to Okapi:

```
curl -w '\n' -X POST -D -   \
   -H "Content-type: application/json"   \
   -d @target/ModuleDescriptor.json \
   http://localhost:9130/_/proxy/modules
```

That ModuleDescriptor tells Okapi what the module is called, what services it
provides, and how to deploy it.

## Deploying the module

Next we need to deploy the module. There is a deployment descriptor in
`target/DeploymentDescriptor.json`. It tells Okapi to start the module on 'localhost'.

Deploy it via Okapi discovery:

```
curl -w '\n' -D - -s \
  -X POST \
  -H "Content-type: application/json" \
  -d @target/DeploymentDescriptor.json  \
  http://localhost:9130/_/discovery/modules
```

Then we need to enable the module for the tenant:

```
curl -w '\n' -X POST -D -   \
    -H "Content-type: application/json"   \
    -d @target/TenantModuleDescriptor.json \
    http://localhost:9130/_/proxy/tenants/<tenant_name>/modules
```

## Tenant Initialization

The module supports v1.2 of the Okapi `_tenant` interface. This version of the interface allows Okapi to pass tenant initialization parameters using the `tenantParameters` key. Currently, the only parameter supported is the `loadReference` key, which will cause the module to load reference data for the tenant if set to `true`.  Here is an example of passing the `loadReference` parameter to the module via Okapi's `/_/proxy/tenants/<tenantId>/install` endpoint:

    curl -w '\n' -X POST -d '[ { "id": "mod-inn-reach-1.1.0", "action": "enable" } ]' http://localhost:9130/_/proxy/tenants/my-test-tenant/install?tenantParameters=loadReference%3Dtrue

This results in a post to the module's `_tenant` API with the following structure:

```json
{
  "module_to": "mod-inn-reach-<VERSION>",
  "parameters": [
    {
      "key": "loadReference",
      "value": "true"
    }
  ]
}
```

See the section [Install modules per tenant](https://github.com/folio-org/okapi/blob/master/doc/guide.md#install-modules-per-tenant) in the Okapi guide for more information.

## Example value for INNREACH_TENANTS
```
For single tenant - tenant1
For multi  tenant - tenant1|tenant2
```

## Additional information

### System user configuration
The module uses system user to communicate with other modules.
For production deployments you MUST specify the password for this system user via env variable:
`SYSTEM_USER_PASSWORD=<password>`.

### Issue tracker

See project [MODINREACH](https://issues.folio.org/projects/MODINREACH)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker/).

### ModuleDescriptor

See the built `target/ModuleDescriptor.json` for the interfaces that this module
requires and provides, the permissions, and the additional module metadata.

### API documentation

This module's [API documentation](https://dev.folio.org/reference/api/#mod-inn-reach).

### Code analysis

[SonarQube analysis](https://sonarcloud.io/dashboard?id=org.folio%3mod-inn-reach).

### Download and configuration

The built artifacts for this module are available.
See [configuration](https://dev.folio.org/download/artifacts) for repository access,
and the [Docker image](https://hub.docker.com/r/folioorg/mod-inn-reach/).

