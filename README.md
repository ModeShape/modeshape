[![License](http://img.shields.io/:license-apache%202.0-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.modeshape/modeshape-parent/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.modeshape%22)
[![Build Status](https://travis-ci.org/ModeShape/modeshape.svg?branch=master)](http://travis-ci.org/ModeShape/modeshape)

Copyright 2008-2016 ModeShape Project.
Licensed under the Apache License, Version 2.0.

# The ModeShape project

This is the official Git repository for the ModeShape project.

ModeShape is an open source implementation of the JCR 2.0 ([JSR-283](http://www.jcp.org/en/jsr/detail?id=283])) specification and standard API.
To your applications, ModeShape looks and behaves like a regular JCR repository. Applications can search, query, navigate, change, version, listen for changes, etc.
But ModeShape can store that content in a variety of back-end stores (including relational databases, the filesystem, etc.), or it can
access and update existing content from *other* kinds of systems (including file systems, Git repositories, JDBC database metadata, and other JCR repositories which support CMIS).
ModeShape's connector architecture means that you can write custom connectors to access any kind of system. And ModeShape can even federate multiple back-end systems
into a single, unified virtual repository.

ModeShape repositories can be used in a variety of applications. One of the most obvious ones is in provisioning and management, where it's critical to 
understand and keep track of the metadata for models, database, services, components, applications, clusters, machines, and other systems used in an enterprise. 
Governance takes that a step farther, by also tracking the policies and expectations against which performance can be verified. In these cases, a repository 
is an excellent mechanism for managing this complex and highly-varied information. But a ModeShape repository doesn't have to be large and complex: it 
could just manage configuration information for an application, or it could just provide a JCR interface on top of a couple of non-JCR systems.

For more information on ModeShape, including getting started guides, reference guides, and downloadable binaries, visit the project's website at [http://www.modeshape.org](http://www.modeshape.org)
or follow us on our [blog](http://modeshape.wordpress.org) or on [Twitter](http://twitter.com/modeshape). Or hop into our [IRC chat room](http://www.jboss.org/modeshape/chat)
and talk our community of contributors and users.

## Get the code

The easiest way to get started with the code is to [create your own fork](http://help.github.com/forking/) of this repository, and then clone your fork:

	$ git clone git@github.com:<you>/modeshape.git
	$ cd modeshape
	$ git remote add upstream git://github.com/ModeShape/modeshape.git
	
At any time, you can pull changes from the upstream and merge them onto your master:

	$ git checkout master               # switches to the 'master' branch
	$ git pull upstream master          # fetches all 'upstream' changes and merges 'upstream/master' onto your 'master' branch
	$ git push origin                   # pushes all the updates to your fork, which should be in-sync with 'upstream'

The general idea is to keep your 'master' branch in-sync with the 'upstream/master'.

## Building ModeShape

Then, we use Maven 3.x to build our software. The following command compiles all the code, installs the JARs into your local Maven repository, and run all of the unit tests:

	$ mvn clean install -s settings.xml

BTW, that '-s settings.xml' argument uses the 'settings.xml' file in our codebase, which is set up to use the JBoss Maven repository

If you are interested in building & contributing to the Wildfly kit as well, you need to run the following command (instead of the earlier command) 
to build all modules, including the Wildfly kit, using our "integration" profile:

    $ mvn clean install -s settings.xml -Pintegration

To build everything, including the ModeShape kit for Wildfly, our JavaDoc, and our other assemblies, use the "assembly" profile instead:

    $ mvn clean install -s settings.xml -Passembly

As an alternative to always passing the "-s settings.xml" parameter, you can modify your local ~/.m2/settings.xml file and add the above mentioned repositories, making sure they are active by default during a build.

That command takes a while -- we do have over 12K unit tests. So if need be, your builds can skip the tests:

	$ mvn clean install -s settings.xml -DskipTests
	
If you have *any* trouble building (or don't like the '-s settings.xml' usage), check the [detailed build instructions and tips](http://community.jboss.org/wiki/ModeShapeAndMaven).

## Contribute fixes and features

ModeShape is licensed under the Apache License 2.0 (see the file LICENSE.txt or [http://www.apache.org/licenses/LICENSE-2.0.html]() for details).

Contributions to ModeShape are welcome. They must be completely authored by you, and you must have the right to contribute them (for example, if you are employed, you must have received the necessary permissions from your employer to make the contribution). All contributions to ModeShape must be licensed under the Apache License 2.0, just like the project itself.

Before committing anything, PLEASE make sure you have set up all of the development tools
(see http://community.jboss.org/wiki/ModeShapeDevelopmentTools), are following the project's
guidelines (see http://community.jboss.org/wiki/ModeShapeDevelopmentGuidelines), and are 
using our accepted workflow (see http://community.jboss.org/wiki/ModeShapeDevelopmentWorkflow).

If you want to fix a bug or make any changes, please log an issue in the [ModeShape JIRA](https://issues.jboss.org/browse/MODE) describing the bug
or new feature. Then we highly recommend making the changes on a topic branch named with the JIRA issue number. For example, this command creates
a branch for the MODE-1234 issue:

	$ git checkout -b mode-1234

After you're happy with your changes and a full build (with unit tests) runs successfully, commit your changes on your topic branch
(using [really good comments](http://community.jboss.org/wiki/ModeShapeDevelopmentGuidelines#Commits)). Then it's time to check for
and pull any recent changes that were made in the official repository:

	$ git checkout master               # switches to the 'master' branch
	$ git pull upstream master          # fetches all 'upstream' changes and merges 'upstream/master' onto your 'master' branch
	$ git checkout mode-1234            # switches to your topic branch
	$ git rebase master                 # reapplies your changes on top of the latest in master
	                                      (i.e., the latest from master will be the new base for your changes)

If the pull grabbed a lot of changes, you should rerun your build to make sure your changes are still good.
You can then either [create patches](http://progit.org/book/ch5-2.html) (one file per commit, saved in `~/mode-1234`) with 

	$ git format-patch -M -o ~/mode-1234 orgin/master

and upload them to the JIRA issue, or you can push your topic branch and its changes into your public fork repository

	$ git push origin mode-1234         # pushes your topic branch into your public fork of ModeShape

and [generate a pull-request](http://help.github.com/pull-requests/) for your changes. 

We prefer pull-requests, because we can review the proposed changes, comment on them,
discuss them with you, and likely merge the changes right into the official repository.
