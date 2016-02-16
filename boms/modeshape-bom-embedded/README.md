This ModeShape BOM makes it easy for your applications and libraries to embed ModeShape and explicitly manage ModeShape's lifecycle. This is typically what is used in the POM files of Java SE applications, libraries, and even web applications that are deployed to containers other than JBoss EAP6. (There is a special BOM for deployments to JBoss EAP 6 that has ModeShape installed.)

== Usage ==

Include the following in your POM file:

    <project>
      ...
      <dependencyManagement>    
        <dependencies>
          <dependency>
            <groupId>org.modeshape.bom</groupId>
            <artifactId>modeshape-bom-embedded</artifactId>
            <version>${version.modeshape}</version>
            <type>pom</type>
            <scope>import</scope>
          </dependency>
        </dependencies>
      </dependencyManagement>
      ...
    </project>

Obviously, you'll need to specify the correct ModeShape version. But note that this is the only place you'll need to specify a version, because the BOM provides a completely valid and consistent set of versions.

This works like all other Maven BOMs by adding into the `dependencyManagement` section all of the dependency defaults for the ModeShape components and dependencies that your module _might_ need. Then, all you have to do is add an explicit dependency to your POM's `dependencies` section for each of ModeShape's components that your module _does_ use, including any of the optional optional components (e.g., sequencers, MongoDB binary store,  JTA implementation, JAAS implementation, etc.) that simply need to be on the classpath for ModeShape to find and use them.

For example, if your module _explicitly_ uses the JCR API and needs to manually set up and manage a ModeShape engine, then simply define these dependencies:

    <project>
      ...
      <dependencies>
        ...
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

You can also add any sequencers and connectors that ModeShape should find. Here's an example that adds in ModeShape's XSD sequencer:

    <project>
      ...
      <dependencies>
        ...
        <dependency>
          <groupId>org.modeshape</groupId>
          <artifactId>modeshape-sequencer-xsd</artifactId>
        </dependency>
        <dependency>
          <groupId>org.modeshape</groupId>
          <artifactId>modeshape-connector-git</artifactId>
        </dependency>
        ...
      </dependencies>
      ...
    </project>