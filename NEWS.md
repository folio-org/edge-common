## 4.0.0 2021-02-26

This releases changes the API for edge-common. There are no known changes
to the HTTP behavior or the configuration of edge-common.

The existing HTTP-based verticle has been renamed to `EdgeVerticleHttp`.
A new verticle wihtout a built-in listener is offered, `EdgeVerticleCore`.

Since edge-common is now based on Vert.x 4.0.0, that will probably
also require updates to module code. Refer to
[4.0.0 Deprecations and breaking changes](https://github.com/vert-x3/wiki/wiki/4.0.0-Deprecations-and-breaking-changes)

Issues pertaining to this release (some of which are related):

 * [EDGCOMMON-5](https://issues.folio.org/browse/EDGCOMMON-5) Merge EdgeVerticle2 -> EdgeVerticle
 * [EDGCOMMON-19](https://issues.folio.org/browse/EDGCOMMON-19) Update dependencies
 * [EDGCOMMON-30](https://issues.folio.org/browse/EDGCOMMON-30) Update to Vert.x 4
 * [EDGCOMMON-31](https://issues.folio.org/browse/EDGCOMMON-31) Allow non-HTTP server
 * [EDGCOMMON-32](https://issues.folio.org/browse/EDGCOMMON-32) EdgeVerticleHttp and update documentaion

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
