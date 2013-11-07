ModeShape provides a number of Maven Bills of Material (BOMs) that make it easier to use ModeShape as a dependency in your own applications. These artifacts specify versions for all of ModeShape's components and dependencies, ensuring you always get a compatible stack.

== ModeShape BOMs ==

The following BOMs are available:

* `modeshape-bom-embedded` - Use this when your applications or components embed ModeShape and explicitly manage ModeShape's lifecycle. This is often what you'll want in Java SE applications, libraries, or even web applications deployed to containers other than JBoss EAP 6.
* `modeshape-bom-jbosseap` - Use this in web applications or services that are to be deployed to JBoss EAP 6 instances that have ModeShape installed as a service.
Your applications never manage the ModeShape lifecycle, but instead just look up one of the repository instances already running within EAP 6.
* `modeshape-bom-remote-client` - Use this when your applications connect to a remote ModeShape server using one of ModeShape's clients libraries: the REST client library or the remote JDBC driver. Most of ModeShape's normal dependencies are not needed, since ModeShape is really running on a remote server.
