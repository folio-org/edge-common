# edge-common

Copyright (C) 2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Common/Shared library for Edge APIs.

## Overview

The intent of edge-common is to simplify the implementation of edge APIs by providing much of the boilerplate code shared among these APIs.

### Usage

1. Extend [EdgeVerticle](https://github.com/folio-org/edge-common/blob/master/src/main/java/org/folio/edge/core/EdgeVerticle.java) and define your API routes by overriding the `public Router defineRoutes()` method.  See [edge-rtac/MainVerticle](https://github.com/folio-org/edge-rtac/blob/master/src/main/java/org/folio/edge/rtac/MainVerticle.java) for details.

   This provides you with all the basics including configuration, initialization of secure store and token cache, etc.

1. Use [InstitutionalUserHelper](https://github.com/folio-org/edge-common/blob/master/src/main/java/org/folio/edge/core/InstitutionalUserHelper.java) for common/shared tasks like parsing API keys, getting OKAPI tokens, etc.
1. Extend or use [OkapiClient](https://github.com/folio-org/edge-common/blob/master/src/main/java/org/folio/edge/core/utils/OkapiClient.java) directly for making calls into FOLIO.

1. Other bits you may find helpful:
 - A generic [Cache](https://github.com/folio-org/edge-common/blob/master/src/main/java/org/folio/edge/core/cache/Cache.java) class (See also: [TokenCache](https://github.com/folio-org/edge-common/blob/master/src/main/java/org/folio/edge/core/cache/TokenCache.java) and [PatronIdCache](https://github.com/folio-org/edge-patron/blob/master/src/main/java/org/folio/edge/patron/cache/PatronIdCache.java))
 - [Mappers](https://github.com/folio-org/edge-common/blob/master/src/main/java/org/folio/edge/core/utils/Mappers.java) class containing static JSON/XML Mapper instances, common date formats, etc.
 - A [MockOkapi](https://github.com/folio-org/edge-common/blob/master/src/main/java/org/folio/edge/core/utils/test/MockOkapi.java) to facilitate mocking in unit tests.
 - Commonly used [Constants](https://github.com/folio-org/edge-common/blob/master/src/main/java/org/folio/edge/core/Constants.java)

Both [edge-patron](https://github.com/folio-org/edge-patron) and [edge-rtac](https://github.com/folio-org/edge-rtac) make extensive use of these features, and serve as examples for how to use edge-common.

## Security

For now, some level of security is achieved via API Keys.  Eventually we may want to implement a more sophisticated security model, e.g. based off of OAuth2, etc.

### API Keys

The API Keys used by the edge APIs are a URL safe base64 encoding of the three pieces of information:

1. Salt - A random string of characters known only to the issuer of the API key, e.g. `nZ56F3LeAa`
1. Tenant ID - A FOLIO tenant ID, e.g. `diku`
1. Institutional Username - The username of the institutional user for this tenant.  This could be the same as the tenant ID, or something else, e.g. `diku` or `dikurtac`, etc.

These components are then concatenated with an underscore delimiter `_` before being base64 encoded, e.g. `nZ56F3LeAa_diku_diku`.

The final API Key looks something like: `blo1NkYzTGVBYV9kaWt1X2Rpa3U=`

The purpose of the salt is to prevent API Key from being guessed, which would be easy if the tenant ID was known, especially if the Institutional Username was the same as the tenant ID.

#### API Key Utilities

A utility class has been provided to help with API key generate, parsing, etc.  The utility can be use programatically, or via a command line interface.  Example CLI usage:

```
$ mvn package

$ java -cp target/edge-common-fat.jar org.folio.edge.core.utils.ApiKeyUtils
Usage: ApiKeyUtils [options]
 -g                   : generate an API Key (default: false)
 -p VAL               : parse an API Key
 -s (--salt-len) N    : the number of salt characters (default: 10)
 -t (--tenant-id) VAL : the tenant's ID
 -u (--username) VAL  : the tenant's institutional user's username

$ java -cp target/edge-common-fat.jar org.folio.edge.core.utils.ApiKeyUtils -g -s 20 -t diku -u diku
QlBhb2ZORm5jSzY0NzdEdWJ4RGhfZGlrdV9kaWt1

$ java -cp target/edge-common-fat.jar org.folio.edge.core.utils.ApiKeyUtils -p QlBhb2ZORm5jSzY0NzdEdWJ4RGhfZGlrdV9kaWt1
Salt: BPaofNFncK6477DubxDh
Tenant ID: diku
Username: diku
```

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

**Key:** `<salt>_<tenantId>_<username>`

e.g. Key=`ab73kbw90e_diku_diku`

#### VaultStore ####

Retrieves credentials from a Vault (https://vaultproject.io).  This was added as a more generic alternative for those not using AWS.  See `src/main/resources/vault.properties`

**Key:** `<salt>/<tenantId>`
**Field:** `<username>`

e.g. Key=`ab73kbw90e/diku`, Field=`diku`

## Configuration

Configuration information is specified in two forms:
1. System Properties - General configuration
1. Properties File - Configuration specific to the desired secure store

### System Properties

Property                 | Default     | Description
------------------------ | ----------- | -------------
`port`                   | `8081`      | Server port to listen on
`okapi_url`              | *required*  | Where to find Okapi (URL)
`secure_store`           | `Ephemeral` | Type of secure store to use.  Valid: `Ephemeral`, `AwsSsm`, `Vault`
`secure_store_props`     | `NA`        | Path to a properties file specifying secure store configuration
`token_cache_ttl_ms`     | `3600000`   | How long to cache JWTs, in milliseconds (ms)
`null_token_cache_ttl_ms`| `30000`     | How long to cache login failure (null JWTs), in milliseconds (ms)
`token_cache_capacity`   | `100`       | Max token cache size
`log_level`              | `INFO`      | Log4j Log Level
`request_timeout_ms`     | `30000`     | Request Timeout

## Additional information

### Issue tracker

See project [FOLIO](https://issues.folio.org/browse/FOLIO)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)

