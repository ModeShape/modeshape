ModeShape provides a number of Maven Bills of Material (BOMs) that make it easier to use ModeShape as a dependency in your own applications. These artifacts specify versions for all of ModeShape's components and dependencies, ensuring you always get a compatible stack.

== ModeShape BOMs ==

The following BOMs are available:

* `modeshape-bom-embedded` - Use this when your applications or components embed ModeShape and explicitly manage ModeShape's lifecycle. This is often what you'll want in Java SE applications, libraries, or even web applications deployed to containers other than JBoss AS7.
* `modeshape-bom-jbossas` - Use this in web applications or services that are to be deployed to JBoss AS7 instances that have ModeShape installed as a service. Your applications never manage the ModeShape lifecycle, but instead just look up one of the repository instances already running within AS7. Therefore, your applications only need access to the JCR API, ModeShape's public API, the Java Transaction API (JTA), and (optionally) the Java Servlet API (used for servlet-based authentication). All these dependences have a scope of `provided` since
they are provided by the ModeShape installation in AS7.
* `modeshape-bom-api` - Use this when your applications or components only depend upon the public APIs that ModeShape provides and uses, namely the JCR API, ModeShape's public API, the Java Transaction API (JTA), and the Java Servlet API (used for servlet-based authentication). However, unlike `modeshape-bom-jbossas`, these dependences default to `compile` scope.
* `modeshape-bom-remote-client` - Use this when your applications connect to a remote ModeShape server using one of ModeShape's clients libraries: the REST client library or the remote JDBC driver. Most of ModeShape's normal dependencies are not needed, since ModeShape is really running on a remote server.
