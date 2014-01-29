ModeShape can be installed into a JBoss Wildfly 8 installation, in which case all configuration and management of the repositories is done via standard Wildfly tools and mechanisms. All your applications and services need to do is simply lookup one of the repositories and use it.

This ModeShape BOM makes it easy for your web applications and services to add dependencies to the JCR API, the ModeShape public API and the Java Transaction API (JTA). (Note that this is similar to the `modeshape-bom-api` BOM, except that all of the dependencies are marked with a scope of `provided`.)

== Usage ==

Include the following in your POM file:

    <project>
      ...
      <dependencyManagement>    
        <dependencies>
          <dependency>
            <groupId>org.modeshape.bom</groupId>
            <artifactId>modeshape-bom-jbossas</artifactId>
            <version>${version.modeshape}</version>
            <type>pom</type>
            <scope>import</scope>
          </dependency>
        </dependencies>
      </dependencyManagement>
      ...
    </project>

Obviously, you'll need to specify the correct ModeShape version. But note that this is the only place you'll need to specify a version, because the BOM provides a completely valid and consistent set of versions.

This works like all other Maven BOMs by adding into the `dependencyManagement` section all of the dependency defaults for the ModeShape components and dependencies that your module _might_ need. Then, all you have to do is add an explicit dependency to your POM's `dependencies` section for each of ModeShape's components that your module _does_ use, including JCR API, JTA, or ModeShape public API. If you need any other APIs, please use one of the Java EE6 BOMs provided by JBoss Wildfly.

For example, if your module uses the JCR API and the ModeShape public API (which is a small extension to the JCR API) then simply define this dependency:

    <project>
      ...
      <dependencies>
        ...
        <dependency>
          <groupId>org.modeshape</groupId>
          <artifactId>modeshape-jcr-api</artifactId>
        </dependency>
        ...
      </dependencies>
      ...
    </project>

Because the ModeShape BOM defines it as `provided`, it will not be included in the web archive for your application or service.

