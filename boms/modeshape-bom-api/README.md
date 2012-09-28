This ModeShape BOM makes it easy for your applications and libraries that need to access only the JCR API, the ModeShape public API, the Java Transaction API (JTA), and the Java Servlet API. You might want to use this in a library that will be handed a Repository instance and will simply use that repository.

== Usage ==

Include the following in your POM file:

    <project>
      ...
      <dependencyManagement>    
        <dependencies>
          <dependency>
            <groupId>org.modeshape.bom</groupId>
            <artifactId>modeshape-bom-api</artifactId>
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

For example, if your module uses the JCR API, the ModeShape public API (which is a small extension to the JCR API), and Java Transaction API, then simply define these dependencies:

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
          <groupId>javax.transaction</groupId>
          <artifactId>jta</artifactId>
        </dependency>
        ...
      </dependencies>
      ...
    </project>

Because the ModeShape BOM defines them as `provided`, they will not be included in the web archive for your application or service.

