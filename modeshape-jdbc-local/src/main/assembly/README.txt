This local JDBC driver JAR requires the JCR API JAR already exists on the classpath.
After all, usage of this local JDBC driver implementation requires a local JCR implementation
that must already use or supply the JCR API.

Also, this JAR uses SLF4J for logging, and includes the SLF4J API. However, this JAR
does not contain the required SLF4J binding JAR nor a logging implementation. Both
of these must be supplied on the classpath before this JAR can be used.