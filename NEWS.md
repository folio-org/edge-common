10/30/2018
 - Added an edge verticle implementation that allows deployment options to be
   set by the verticle launcher. This required moving much of the code from
   the constructor into the `start` method. This allows the launcher to provide
   a vertx to the verticle which contains the deployment options. It also allows
   the verticle to perform time consuming tasks without blocking the main
   thread.
08/16/2018
 - The source of the API Key is now configurable via the `api_key_sources` system property.  See [README.md](README.md) for details.
