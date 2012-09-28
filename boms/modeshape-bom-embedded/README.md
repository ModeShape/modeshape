This ModeShape BOM makes it easy for your applications and libraries to embed ModeShape and explicitly manage ModeShape's lifecycle. This is typically what is used in the POM files of Java SE applications, libraries, and even web applications that are deployed to containers other than JBoss AS7. (There is a special BOM for deployments to JBoss AS7 that has ModeShape installed.)

== Usage ==

Include the following in your POM file:

    <project>
      ...
      <dependencyManagement>    
        <dependencies>
          <dependency>
            <groupId>org.modeshape.bom</groupId>
            <artifactId>modeshape-bom-embedded</artifactId>
            <version>3.0.0.Final</version>
            <type>pom</type>
            <scope>import</scope>
          </dependency>
        </dependencies>
      </dependencyManagement>
      ...
    </project>

Obviously, you'll need to specify the correct ModeShape version. But note that this is the only place you'll need to specify a version, because the BOM provides a completely valid and consistent set of versions.

This works like all other Maven BOMs by adding into the `dependencyManagement` section all of the dependency defaults for the ModeShape components and dependencies that your module _might_ need. Then, all you have to do is add an explicit dependency to your POM's `dependencies` section for each of ModeShape's components that your module _does_ use, including any of the optional optional components (e.g., sequencers, MongoDB binary store, Infinispan cache stores, JTA implementation, JAAS implementation, etc.) that simply need to be on the classpath for ModeShape to find and use them.

For example, if your module _explicitly_ uses the JCR API and needs to manually set up and manage a ModeShape engine, then simply define these dependencies:

    <project>
      ...
      <dependencies>
        ...
        <dependency>
          <groupId>javax.jcr</groupId>
          <artifactId>jcr</artifactId>
        </dependency>
        <dependency>
          <groupId>org.modeshape</groupId>
          <artifactId>modeshape-jcr-api</artifactId>
        </dependency>
        <dependency>
          <groupId>org.modeshape</groupId>
          <artifactId>modeshape-jcr</artifactId>
        </dependency>
        ...
      </dependencies>
      ...
    </project>

You can also add any sequencers that ModeShape should find. Here's an example that adds in ModeShape's XSD sequencer:

    <project>
      ...
      <dependencies>
        ...
        <dependency>
          <groupId>org.modeshape</groupId>
          <artifactId>modeshape-sequencer-xsd</artifactId>
        </dependency>
        ...
      </dependencies>
      ...
    </project>

You should do the same for the Infinispan cache stores, MongoDB driver, or other optional components. For example, if you're writing a Java SE application, you can even include the Java Transaction API (JTA) and a fully-functioning implementation:

    <project>
      ...
      <dependencies>
        ...
        <dependency>
          <groupId>javax.transaction</groupId>
          <artifactId>jta</artifactId>
        </dependency>
        <dependency>
          <groupId>org.jboss.jbossts</groupId>
          <artifactId>jbossjta</artifactId>
        </dependency>
        ...
      </dependencies>
      ...
    </project>

ModeShape can use a variety of logging frameworks. If your application uses the JDK's logging framework, do not include any logging dependencies.

If your application uses SLF4J, simply include the API, the binding JAR, and the logging implementation. For example, adding these dependencies would use Log4J via SLF4J, where both ModeShape and your application can use SLF4J.  to use a logging library, simply include *_one_* of the following dependencies:

    <project>
      ...
      <dependencies>
        ...
        <dependency>
          <groupId>org.slf4j</groupId>
          <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
          <groupId>org.slf4j</groupId>
          <artifactId>slf4j-log4j12</artifactId>
        </dependency>
        <dependency>
          <groupId>log4j</groupId>
          <artifactId>log4j</artifactId>
        </dependency>
        ...
      </dependencies>
      ...
    </project>

If your application directly uses Log4J, then simply add that dependency:

    <project>
      ...
      <dependencies>
        ...
        <dependency>
          <groupId>log4j</groupId>
          <artifactId>log4j</artifactId>
        </dependency>
        ...
      </dependencies>
      ...
    </project>

Or, if your application directly uses LogBack (which implements the SLF4J API), simply include the following dependencies:

    <project>
      ...
      <dependencies>
        ...
        <dependency>
          <groupId>org.slf4j</groupId>
          <artifactId>slf4j-api</artifactId>
        </dependency>
        <dependency>
          <groupId>ch.qos.logback</groupId>
          <artifactId>logback-classic</artifactId>
          <version>${logback.version}</version>
        </dependency>
        ...
      </dependencies>
      ...
    </project>

