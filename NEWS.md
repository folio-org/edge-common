## 4.7.2 2024-09-03
* [EDGCOMMON-83](https://folio-org.atlassian.net/browse/EDGCOMMON-82) Enable Hostname verification enabled for Http Clients when using TLS connections


## 4.7.1 2024-07-01
* [EDGCOMMON-82](https://folio-org.atlassian.net/browse/EDGCOMMON-82) Configure approved SecureRandom in FIPS mode

## 4.7.0 2024-05-23
[Full Changelog](https://github.com/folio-org/edge-common/compare/v4.6.0...v4.7.0)

## Stories
* [EDGCOMMON-80](https://folio-org.atlassian.net/browse/EDGCOMMON-80) - Vert.x 4.5.7, Netty 4.1.108.Final fixing form post OOM CVE-2024-29025 
* [EDGCOMMON-78](https://folio-org.atlassian.net/browse/EDGCOMMON-78) - Enhance HTTP Endpoint Security with TLS and FIPS-140-2 Compliant Cryptography
* [EDGCOMMON-77](https://folio-org.atlassian.net/browse/EDGCOMMON-77) - Support HTTPS in OkapiClient, SSL


## 4.6.0 2024-03-11

 * [EDGCOMMON-75](https://folio-org.atlassian.net/browse/EDGCOMMON-75) Quesnelia dep upgrades: Vert.x 4.5.4, aws 1.12.671, vault 6.2.0, … 

## 4.5.3 2024-03-04

 * [EDGCOMMON-74](https://folio-org.atlassian.net/browse/EDGCOMMON-74) aws-java-sdk-ssm 1.12.645 removing ion-java 1.0.2

## 4.5.2 2023-11-06

 * [EDGCOMMON-72](https://issues.folio.org/browse/EDGCOMMON-72) Vert.x 4.4.6, Netty 4.1.100.Final

## 4.5.1 2023-10-06

 * [EDGCOMMON-70](https://issues.folio.org/browse/EDGCOMMON-70) Failure to find org.apache.maven.plugins:maven-deploy-plugin:jar:3.6.0

## 4.5.0 2023-10-05

 * [EDGCOMMON-57](https://issues.folio.org/browse/EDGCOMMON-57) Use refresh token aware client from okapi-common
 * [EDGCOMMON-64](https://issues.folio.org/browse/EDGCOMMON-64) Java 17
 * [EDGCOMMON-67](https://issues.folio.org/browse/EDGCOMMON-67) Provide a consortia-level access point/mechanism for retrieving patron data
 * [EDGCOMMON-68](https://issues.folio.org/browse/EDGCOMMON-68) Poppy dependency upgrades: Vert.x 4.4.5, ...

## 4.4.3 2023-02-20

 * [EDGCOMMON-59](https://issues.folio.org/browse/EDGCOMMON-59) Explain header injection in OkapiClient javadoc
 * [EDGCOMMON-63](https://issues.folio.org/browse/EDGCOMMON-63) Vert.x 4.3.8, okapi-common 4.14.11, aws-java-sdk-ssm 1.12.408, httpcore 4.4.16

## 4.4.2 2022-11-24

 * [EDGCOMMON-56](https://issues.folio.org/browse/EDGCOMMON-56) Upgrade all dependencies; Jackson 2.14.0 fixes CVE-2022-42003

## 4.4.1 2022-08-17

 * [EDGCOMMON-54](https://issues.folio.org/browse/EDGCOMMON-54) Vert.x 4.3.3 fixing disabled SSL in 4.3.0/4.3.1

## 4.4.0 2022-07-25

 * [EDGCOMMON-51](https://issues.folio.org/browse/EDGCOMMON-51) Futurize MockOkapi for Junit 5 and .compose
 * [EDGCOMMON-52](https://issues.folio.org/browse/EDGCOMMON-52) Enable "Accept-Encoding: deflate, gzip" by default
 * [EDGCOMMON-53](https://issues.folio.org/browse/EDGCOMMON-53) Deprecate X-Duration, provide setDelay, fix timeout reporting

Upgrading notes:

Edge module must no longer copy HTTP headers from the incoming request to the outgoing
request. Remove deny list code like `ctx.request().headers().remove(CONTENT_LENGTH)`. For security
use an allow list to copy only the HTTP headers that really are needed, if any. Support for this has
been added to edge-common's Handler and MockOkapi classes.

Replace
```
mockOkapi.start(testContext)
```
with
```
mockOkapi.start()
.onComplete(testContext.asyncAssertSuccess());
```
and
```
mockOkapi.close(testContext);
```
with
```
mockOkapi.close()
.onComplete(context.asyncAssertSuccess());
```

## 4.3.0 2022-06-02

 * [EDGCOMMON-49](https://issues.folio.org/browse/EDGCOMMON-49) Upgrade dependencies: Vert.x 4.3.1, ...
 * [EDGCOMMON-48](https://issues.folio.org/browse/EDGCOMMON-48) Remove vertx-completable-future
 * [EDGCOMMON-47](https://issues.folio.org/browse/EDGCOMMON-47) Fix behavior when tenant header is present in a request]
 * [EDGCOMMON-46](https://issues.folio.org/browse/EDGCOMMON-46) Vert.x 4.2.7 fixing jackson-databind DoS (CVE-2020-36518)
 * [EDGCOMMON-45](https://issues.folio.org/browse/EDGCOMMON-45) Reuse WebClient for pooling, pipe-lining, multiplexing
 * [EDGCOMMON-43](https://issues.folio.org/browse/EDGCOMMON-43) Update dependencies including Vert.x 4.2.4, Log4j 2.17.1
 * [EDGCOMMON-42](https://issues.folio.org/browse/EDGCOMMON-43) cryptographically strong random for token and salt
 * [EDGCOMMON-40](https://issues.folio.org/browse/EDGCOMMON-40) Upgrade to log4j 2.17.0
 * [EDGCOMMON-38](https://issues.folio.org/browse/EDGCOMMON-38) Upgrade to log4j 2.16.0, Vert.x 4.2.2
 * [EDGCOMMON-21](https://issues.folio.org/browse/EDGCOMMON-21) Do not block the Vert.x main thread when retrieving data from the secure store

## 4.2.0 2021-07-13

* Defines request timeout when making HTTP requests (EDGCOMMON-36)

## 4.1.0 2021-06-02

No changes to the API since last release.

 * [EDGCOMMON-34](https://issues.folio.org/browse/EDGCOMMON-34) Upgrade to Vert.x 4.1.0

## 4.0.0 2021-02-26

This releases changes the API for edge-common. There are no known changes
to the HTTP behavior or the configuration of edge-common.

The existing HTTP-based verticle has been renamed to `EdgeVerticleHttp`.
A new verticle without a built-in listener is offered, `EdgeVerticleCore`.

Since edge-common is now based on Vert.x 4.0.0, that will probably
also require updates to module code. Refer to
[4.0.0 Deprecations and breaking changes](https://github.com/vert-x3/wiki/wiki/4.0.0-Deprecations-and-breaking-changes)

Issues pertaining to this release (some of which are related):

 * [EDGCOMMON-5](https://issues.folio.org/browse/EDGCOMMON-5) Merge EdgeVerticle2 -> EdgeVerticle
 * [EDGCOMMON-19](https://issues.folio.org/browse/EDGCOMMON-19) Update dependencies
 * [EDGCOMMON-30](https://issues.folio.org/browse/EDGCOMMON-30) Update to Vert.x 4
 * [EDGCOMMON-31](https://issues.folio.org/browse/EDGCOMMON-31) Allow non-HTTP server
 * [EDGCOMMON-32](https://issues.folio.org/browse/EDGCOMMON-32) EdgeVerticleHttp and update documentation

## 3.1.0 2021-01-26

There are no known breaking changes in this release, but modules should also
upgrade to Java 11 and Log4j2 while using this release.

 * [EDGCOMMON-23](https://issues.folio.org/browse/EDGCOMMON-23) Update edge-common to use Log4j2
 * [EDGCOMMON-24](https://issues.folio.org/browse/EDGCOMMON-24) Update mockito
 * [EDGCOMMON-28](https://issues.folio.org/browse/EDGCOMMON-28) Upgrade to Java 11

## 3.0.0 2020-10-29

*IMPORTANT*: This release introduces breaking changes. There will be a single instance of okapi client per OkapiClientFactory and per tenant, which means that this client should never be closed or else there will be runtime errors. To enforce this behaviour, method close() has been removed from OkapiClient class.

 * [EDGCOMMON-25](https://issues.folio.org/browse/EDGCOMMON-25): OkapiClientFactory is leaking HttpClient objects

## 2.0.2 2019-07-10

 * [EDGCOMMON-20](https://issues.folio.org/browse/EDGCOMMON-20): Enable the use of compression by
   the HTTP client

## 2.0.1

Complete [Changelog](https://github.com/folio-org/edge-common/compare/v2.0.0...v2.0.1)

 * [EDGCOMMON-19](https://issues.folio.org/browse/EDGCOMMON-19): ApiKeyUtils jar
   missing dependencies

## 2.0.0

*IMPORTANT*: This release introduces breaking changes related to API keys.  Any
API Keys in use will need to be re-issued once your edge API is upgraded to 
edge-common v2.0.0.

 * [EDGCOMMON-18](https://issues.folio.org/browse/EDGCOMMON-18): Restructure API
   keys to work with tenantIds/usernames containing underscore/special characters.
 * [EDGCOMMON-17](https://issues.folio.org/browse/EDGCOMMON-17): ApiKeyUtils jar 
   missing dependency
 * [EDGCOMMON-13](https://issues.folio.org/browse/EDGCOMMON-13): Test set the
   region for the mocked AWS param store service to avoid a potential missing
   region exception
 * [EDGCOMMON-12](https://issues.folio.org/browse/EDGCOMMON-12): Tests now wait
   for related server shutdown completion before moving to another test
 * [EDGCOMMON-11](https://issues.folio.org/browse/EDGCOMMON-11): A shaded fat
   jar is no longer produced by the build
   * However, we do still need a jar with the `args4j` dependency and the
     `ApiKeyUtils` class and related classes so we can generate API keys from
     the command line. We now generate an executable jar file that can be used
     to generate/parse API keys.
 * [EDGCOMMON-7](https://issues.folio.org/browse/EDGCOMMON-7): Changed the
   dependency to use the core args4j instead of the maven plugin
 * [EDGCOMMON-4](https://issues.folio.org/browse/EDGCOMMON-4): Locked in the
   vertx version using dependency management

## 1.0.0
 * First formal release of all functionality implemented up to this point.

10/30/2018
 - Added an edge verticle implementation that allows deployment options to be
   set by the verticle launcher. This required moving much of the code from
   the constructor into the `start` method. This allows the launcher to provide
   a vertx to the verticle which contains the deployment options. It also allows
   the verticle to perform time consuming tasks without blocking the main
   thread.
08/16/2018
 - The source of the API Key is now configurable via the `api_key_sources` system 
   property.  See [README.md](README.md) for details.

8/11/2018
 - Artifact version edge-common 0.3.8-SNAPSHOT introduces changes related to 
   `ephemeral.properties` records format.  For each tenant, the institutional user 
   and password should be written in format:

   `{tenant}={user},{password}`

   instead of previously used:

   `tenant={password}`.
