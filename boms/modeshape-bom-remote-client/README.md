This ModeShape BOM makes it easy for your applications and libraries to connect to a remote ModeShape repository, using either ModeShape's REST API or JDBC driver.

== Usage ==

Include the following in your POM file:

    <project>
      ...
      <dependencyManagement>    
        <dependencies>
          <dependency>
            <groupId>org.modeshape.bom</groupId>
            <artifactId>modeshape-bom-remote-client</artifactId>
            <version>3.0.0.Final</version>
            <type>pom</type>
            <scope>import</scope>
          </dependency>
        </dependencies>
      </dependencyManagement>
      ...
    </project>

Obviously, you'll need to specify the correct ModeShape version. But note that this is the only place you'll need to specify a version, because the BOM provides a completely valid and consistent set of versions.

This works like all other Maven BOMs by adding into the `dependencyManagement` section all of the dependency defaults for the ModeShape components and dependencies that your module _might_ need. Then, all you have to do is add an explicit dependency to your POM's `dependencies` section for each of ModeShape's components that your module _does_ use.

For example, if your module _explicitly_ uses ModeShape's REST client library, then simply define these dependencies:

    <project>
      ...
      <dependencies>
        ...
        <dependency>
            <groupId>org.modeshape</groupId>
            <artifactId>modeshape-web-jcr-rest-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.modeshape</groupId>
            <artifactId>modeshape-jdbc</artifactId>
        </dependency>
        ...
      </dependencies>
      ...
    </project>

There are quite a few other transitive dependencies of these components.

