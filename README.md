# mod-inn-reach

Copyright (C) 2021-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

<!-- ../../okapi/doc/md2toc -l 2 -h 4 README.md -->
* [Introduction](#introduction)
* [Environment variables](#environment-variables)
* [Compiling](#compiling)
* [Docker](#docker)
* [Installing the module](#installing-the-module)
* [Tenant Initialization](#tenant-initialization)
* [Example value for INNREACH_TENANTS](#example-value-for-innreach_tenants)
* [Circulation](#circulation)
* [Additional information](#additional-information)

## Introduction

The module provides an access to INN-Reach.

### Environment variables:

| Name                                            |       Default value       | Description                                                                                                                                                                                                                                                                                                                  |
|:------------------------------------------------|:-------------------------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| JAVA_OPTIONS                                    | -XX:MaxRAMPercentage=85.0 | Java options                                                                                                                                                                                                                                                                                                                 |
| DB_HOST                                         |         postgres          | Postgres hostname                                                                                                                                                                                                                                                                                                            |
| DB_PORT                                         |           5432            | Postgres port                                                                                                                                                                                                                                                                                                                |
| DB_USERNAME                                     |        folio_admin        | Postgres username                                                                                                                                                                                                                                                                                                            |
| DB_PASSWORD                                     |             -             | Postgres password                                                                                                                                                                                                                                                                                                            |
| DB_DATABASE                                     |       okapi_modules       | Postgres database name                                                                                                                                                                                                                                                                                                       |
| DB_QUERYTIMEOUT                                 |           60000           | Database query timeout                                                                                                                                                                                                                                                                                                       |
| DB_CHARSET                                      |           UTF-8           | Database charset                                                                                                                                                                                                                                                                                                             |
| DB_MAXPOOLSIZE                                  |             5             | Database max pool size                                                                                                                                                                                                                                                                                                       |
| OKAPI_URL                                       |     http://okapi:9130     | OKAPI URL used to login system user, required                                                                                                                                                                                                                                                                                |
| ENV                                             |           folio           | Logical name of the deployment, must be set if Kafka/Elasticsearch are shared for environments, `a-z (any case)`, `0-9`, `-`, `_` symbols only allowed                                                                                                                                                                       |
| SYSTEM_USER_ENABLED                             |           true            | Defines if system user must be created during service tenant initialization                                                                                                                                                                                                                                                  |
| SYSTEM_USER_NAME                                |       mod-innreach        | Username of System user                                                                                                                                                                                                                                                                                                      |
| SYSTEM_USER_PASSWORD                            |             -             | Internal user password                                                                                                                                                                                                                                                                                                       |
| KAFKA_HOST                                      |           kafka           | Kafka broker hostname                                                                                                                                                                                                                                                                                                        |
| KAFKA_PORT                                      |           9092            | Kafka broker port                                                                                                                                                                                                                                                                                                            |
| KAFKA_SECURITY_PROTOCOL                         |         PLAINTEXT         | Kafka security protocol used to communicate with brokers (SSL or PLAINTEXT)                                                                                                                                                                                                                                                  |
| KAFKA_SSL_KEYSTORE_LOCATION                     |             -             | The location of the Kafka key store file. This is optional for client and can be used for two-way authentication for client.                                                                                                                                                                                                 |
| KAFKA_SSL_KEYSTORE_PASSWORD                     |             -             | The store password for the Kafka key store file. This is optional for client and only needed if 'ssl.keystore.location' is configured.                                                                                                                                                                                       |
| KAFKA_SSL_TRUSTSTORE_LOCATION                   |             -             | The location of the Kafka trust store file.                                                                                                                                                                                                                                                                                  |
| KAFKA_SSL_TRUSTSTORE_PASSWORD                   |             -             | The password for the Kafka trust store file. If a password is not set, trust store file configured will still be used, but integrity checking is disabled.                                                                                                                                                                   |
| LOG_HTTP                                        |           false           | Enable logging of all requests and responses.                                                                                                                                                                                                                                                                                |
| DEFAULT_INTERVAL                                |           95000           | Default interval in ms ( preffred not to increase more than this value )                                                                                                                                                                                                                                                     |
 | MAX_FAILURE                                     |            360            | Default max attempts                                                                                                                                                                                                                                                                                                         |
| DEFAULT_OFFSET                                  |          latest           | Default kafka offset                                                                                                                                                                                                                                                                                                         |
 | DEFAULT_CONCURRENCY                             |             2             | Deafult concurrency of kafka consumer                                                                                                                                                                                                                                                                                        |
| INNREACH_TENANTS                                |             -             | Pipe-delimited list of tenant names that mod-inn-reach processes Kafka events for. See [Example value for INNREACH_TENANTS](#example-value-for-innreach_tenants)                                                                                                                                                            |
| CONTRIBUTION_POOL_SIZE                          |            50             | Thread pool size of scheduler task executor, both for initial and ongoing contribution                                                                                                                                                                                                                                       |
| CONTRIBUTION_SCHEDULER_DELAY                    |           10000           | Time interval between scheduler runs of Contribution job, value should be given in milli seconds                                                                                                                                                                                                                             |
| INITIAL_CONTRIBUTION_SCHEDULER_DELAY            |           10000           | Same interval as `CONTRIBUTION_SCHEDULER_DELAY` but for initial contribution job scheduler, value should be given in milli seconds                                                                                                                                                                                           |
| CONTRIBUTION_RETRIES                            |           3600            | Max Retry attempts. If the value is given as 0, then it will be considered as indefinite retry.                                                                                                                                                                                                                              |
| CONTRIBUTION_FETCH_LIMIT                        |            50             | Number of records that needs to fetch for every scheduler run, both for initial and ongoing contribution                                                                                                                                                                                                                     |
| CONTRIBUTION_ITEM_PAUSE                         |             1             | Time delay between Instance contribution and item initial contribution. The value should be given in hrs.                                                                                                                                                                                                                    |
| INN_REACH_HTTP_CLIENT_CONNECTION_TIMEOUT        |          120000           | INN-Reach http client connection timeout in milliseconds                                                                                                                                                                                                                                                                     |
| INN_REACH_HTTP_CLIENT_READ_TIMEOUT              |           60000           | INN-Reach http client read timeout in milliseconds                                                                                                                                                                                                                                                                           |
| CONTRIBUTION_MIN_BATCH_THRESHOLD                |             5             | Minimum number of available slots (`fetch-limit - inProgressCount`) required before claiming a new batch. If available slots are below this threshold, the scheduler skips the tick for that tenant. Tune together with `CONTRIBUTION_FETCH_LIMIT` and `CONTRIBUTION_SCHEDULER_DELAY`/`INITIAL_CONTRIBUTION_SCHEDULER_DELAY` |

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
# Single tenant
INNREACH_TENANTS=tenant1

# Multiple tenants (pipe-delimited)
INNREACH_TENANTS=tenant1|tenant2|tenant3
```

## Circulation

### patronId Lifecycle

The `patronId` field in D2IR circulation request bodies is a **Base32-encoded FOLIO user UUID**.

#### Encoding

When FOLIO responds to a patron verification request (`POST /d2ir/circ/verifypatron`), it encodes the patron's UUID using Base32 (lowercase, no padding) via `UUIDEncoder.encode(user.getId())`. This produces a 26-character lowercase alphanumeric string.

#### Flow

1. Central Server calls `POST /d2ir/circ/verifypatron` — FOLIO returns `patronId` (Base32-encoded UUID) in the `PatronInfo` response.
2. Central Server stores this value and includes it in all subsequent D2IR circulation requests for that patron (item holds, patron holds, local holds, etc.).
3. When FOLIO receives a D2IR request containing `patronId`, it decodes the Base32 string back to a UUID via `UUIDEncoder.decode(patronId)` and uses it to look up the FOLIO user record.

#### Where it is actively decoded and used

- `POST /d2ir/circ/patronhold` — decoded to look up patron for virtual record creation
- `PUT /d2ir/circ/localhold` — decoded to look up patron as requester for item request
- `POST /d2ir/circ/itemhold` — stored in the transaction; decoded later by lifecycle endpoints (`recall`, `ownerrenew`, `transferrequest`, etc.) that call `populateTransactionPatronInfo`

#### Schema validation

The field is validated by the pattern `[a-z,0-9]{1,32}` which accepts lowercase alphanumeric strings up to 32 characters.

### title Usage

The `title` field in D2IR circulation request bodies represents the **bibliographic title of the requested item**.

#### Virtual Instance creation (Patron Hold flow)

In the patron hold flow (`POST /d2ir/circ/patronhold`), the `title` value is used as the title of the **virtual Instance** created in FOLIO inventory. Since `title` is a required field for Instance creation in `mod-inventory-storage`, a missing or empty `title` in the patron hold request will result in a 422 error from the inventory API.

#### Other flows

In other flows, `title` is either overwritten from the actual inventory item's title (`itemhold`) or not used in processing logic (`localhold`, `itemshipped`, `itemreceived`, `returnuncirculated`).

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

