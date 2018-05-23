# edge-common

Copyright (C) 2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Common/Shared library for Edge APIs.

## Overview

TBD

## Security

TBD

### Institutional Users

The idea here is that a FOLIO user is created for each tenant for the purposes of edge APIs.  The credentials are stored in one of the secure stores and retrieved as needed by the edge API.

The Edge API does not create users, or write credentials.  Those need to be provisioned manually or by some other process.  The current secure stores expect credentials to be stored in a way that adheres to naming conventions.  See the various secure store sections below for specifics.

Currently the institutional username is the same as the tenantId, but this is subject to change.

### Secure Stores

Three secure stores currently implemented for safe retrieval of encrypted credentials:

#### EphemeralStore ####

Only intended for _development purposes_.  Credentials are defined in plain text in a specified properties file.  See `src/main/resources/ephemeral.properties`

#### AwsParamStore ####

Retrieves credentials from Amazon Web Services Systems Manager (AWS SSM), more specifically the Parameter Store, where they're stored encrypted using a KMS key.  See `src.main/resources/aws_ss.properties`

**Key:** `<tenantId>_<username>`

e.g. Key=`diku_diku`

#### VaultStore ####

Retrieves credentials from a Vault (https://vaultproject.io).  This was added as a more generic alternative for those not using AWS.  See `src/main/resources/vault.properties`

**Key:** `secrets/<tenantId>`
**Field:** `<username>`

e.g. Key=`secrets/diku`, Field=`diku`

## Configuration

Configuration information is specified in two forms:
1. System Properties - General configuration
1. Properties File - Configuration specific to the desired secure store

### System Properties

Proprety              | Default     | Description
--------------------- | ----------- | -------------
`port`                | `8081`      | Server port to listen on
`okapi_url`           | *required*  | Where to find Okapi (URL)
`secure_store`        | `Ephemeral` | Type of secure store to use.  Valid: `Ephemeral`, `AwsSsm`, `Vault`
`secure_store_props`  | `NA`        | Path to a properties file specifying secure store configuration
`token_cache_ttl_ms`  | `3600000`   | How long to cache JWTs, in milliseconds (ms)
`token_cache_capacity`| `100`       | Max token cache size
`log_level`           | `INFO`      | Log4j Log Level

## Additional information

### Issue tracker

See project [FOLIO](https://issues.folio.org/browse/FOLIO)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)

