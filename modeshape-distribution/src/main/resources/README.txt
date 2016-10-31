Copyright 2008-2016 ModeShape Project.
Licensed under the Apache License, Version 2.0.

# Overview

ModeShape is an open source implementation of the JCR 2.0 ([JSR-283](http://www.jcp.org/en/jsr/detail?id=283])) specification and standard API.
To your applications, ModeShape looks and behaves like a regular JCR repository. Applications can search, query, navigate, change, version, listen for changes, etc.
But ModeShape can store that content in a variety of back-end stores (including relational databases, the filesystem, etc.), or it can
access and update existing content from *other* kinds of systems (including file systems, Git repositories, JDBC database metadata, and other JCR repositories which support CMIS).
ModeShape's connector architecture means that you can write custom connectors to access any kind of system. And ModeShape can even federate multiple back-end systems
into a single, unified virtual repository.

For the complete documentation, please visit: https://docs.jboss.org/author/display/MODE/Home

# Using ModeShape

By far the easiest way to use ModeShape is to use Maven, because with just a few lines of code, Maven will automatically pull
all the JARs and source for all of the ModeShape libraries as well as everything those libraries need.
For all the details, please visit http://www.jboss.org/modeshape/downloads/maven

If you're not using Maven, the easiest way use ModeShape is via the files in this distribution, which have the following structure:

/- modeshape-version

    /- client - the folder containing the JAR that can be used to connect and interact with ModeShape remotely, via JDBC. 
                See https://docs.jboss.org/author/display/MODE50/Using+Repositories+with+JDBC+in+Wildfly for more information

    /- doc - the documentations folder, which will contain among other things, the full javadoc

    /- jca - the folder containing ModeShape's JCA adapter. See https://docs.jboss.org/author/display/MODE50/ModeShape%27s+JCA+Adapter
             for more information

    /- lib - the folder containing all the runtime-dependencies of the core components   

    /- modules - the root folder of all the "extra" modules, which can be used on top of ModeShape's core.

        /- module_1

            /- lib - the folder containing the module's specific dependencies
            - module_1.jar - the main module artifacts
            - module_1-sources.jar - the sources for this module
            - any additional readme(s), copyright and authors file, specific to any module

       /- module_2

   - modeshape-core-jars - represent the jars which constitute ModeShape's core.
                           These are the minimum required ModeShape dependencies when running standalone or inside a container
                           not yet supported by ModeShape.

   - AUTHORS.txt
   - COPYRIGHT.txt
   - README.txt
   - LICENSE.txt

# Running the Examples

ModeShape has an extensive set of examples both using the standalone repository or using the repository in JBoss AS. You can check 
out these examples at their respective GitHub repositories: https://github.com/ModeShape/modeshape-examples and https://github.com/ModeShape/quickstart.