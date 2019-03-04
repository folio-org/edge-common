## 2.0.2 - Unreleased

## 2.0.1

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
