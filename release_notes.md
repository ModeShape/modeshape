# Release Notes for ModeShape &version;

This document outlines the changes that were made in ModeShape &version;.
We hope you enjoy it!

## What's new

ModeShape 4 is a major upgrade over 3.x and offers significant improvements in clustering, performance, query, and events.
All JCR 2.0 (JSR-283) features are supported, and ModeShape 4 has complete integration with Wildfly 8, allowing deployed applications 
to simply lookup and use repositories managed by ModeShape's service.

ModeShape &version; is licensed under the Apache Software License, 2.0.

This release addresses 18 bugs and 11 enhancements, the most important of which being that of moving to Infinispan 7.2.0.Final.
This version of Infinispan has serveral fixes and improvements since Infinispan 6, including a fix for [MODE-2280](https://issues.jboss.org/browse/MODE-2280).
This change meant, however, that our Wildfly integration kit had to be adapted in order to be able to integrate with Infinispan 
in library mode rather than via the Wildfly subsystem.

** If you're using the Wildfly kit, when moving to ModeShape &version; you will have to update your cache configuration 
as described [in this section of the documentation](https://docs.jboss.org/author/display/MODE40/Configuring+ModeShape+in+Wildfly#ConfiguringModeShapeinWildfly-MigratingfromModeShape4.2%28orlower%29toModeShape4.3%28orgreater%29)**

In addition to the Infinispan related changes, we've enhanced the [Backup & Restore API](https://docs.jboss.org/author/display/MODE40/Backup+and+restore)
allowing a more fine-grained control over what parts of a repository are backed up and then restored. We've also added the ability
to perform a full repository backup & restore [via the REST Service](https://docs.jboss.org/author/display/MODE40/REST+Service#RESTService-26.Backuparepository)

## Features

ModeShape &version; has these features:

- ModeShape uses Infinispan for all caching and storage, giving a powerful and flexible
foundation for creating JCR repositories that are fast, scalable, and highly available.
Infinispan offers a great deal of storage options (via cache loaders), but using Infinispan 
as a distributed, multi-site, in-memory data grid provides incredible scalability and performance.
- Strongly consistent. ModeShape is atomic, consistent, isolated and durable (ACID), so writing
applications is very natural. Applications can even use JTA transactions.
- Fast. ModeShape 4 is just plain seriously fast, and performance is all-around
faster than earlier version.
- Larger content. ModeShape 4 can store and access the content so that
a node can have hundreds of thousands (or more!) of child nodes (even with same-name-siblings)
yet still be incredibly fast. Additionally, repositories can scale to many millions of nodes 
and be deployed across many processes.
- Simplified configuration. There is no more global configuration of the engine; instead,
each repository is configured with a separate JSON file, which must conform to a ModeShape-specific
JSON Schema and can be validated by ModeShape prior to use. Repository configurations can even be
changed while the repository is running (some restrictions apply), making it possible to 
add/change/remove sequencers, authorization providers, and many other configuration options
while the repository is in use.
- Elastic. Add processes to scale out, without having to have a single coordinator.
- Deploy, start, stop and undeploy repositories while the engine is running and while and other
repositories are still in use.
- Sessions immediately see all changes persisted/committed by other sessions, although
transient changes made by the session always take precedence.
- Monitoring API with over a dozen metrics.
- Sequencing SPI that uses the JCR API to get at the content being processed and create/update the 
derived content. Sequencers can also dynamically register namespaces and node types. Now it's easy 
to create custom sequencers.
- Connector SPI that defines how external systems are accessed and optionally updated to project
the external information into the repository as regular nodes.
- Simple API for implementing custom MIME type detectors. ModeShape still has a built-in
Tika-based detector that determines MIME types using the filename extensions and binary content.
- Simple API for implementing custom text extractors, which extracts from binary values
searchable text used in full-text searches and queries.
- Ability to store binary values of any sizes, with a separate facility for storing these on the file
system, in Infinispan caches, in relational DBMSes (via JDBC), and in MongoDB. Custom stores are also
possible.
- Public API interfaces and methods that were deprecated in 2.7.0.Final (or later) have been removed.
There weren't many of these; most of the ModeShape API remains the same as 2.x.
- Integration with JBoss Wildfly 8. ModeShape runs as an integrated subsystem within Wildfly, and
the Wildfly tooling can be used to define and manage repositories independently of each other
and while the server is running.
- Local and remote JDBC drivers for issuing JCR-SQL2 queries and getting database metadata via the JDBC API
- Use the RESTful API to talk to ModeShape repositories from non-Java and non-local applications
- Use the CMIS API to talk to ModeShape repositories
- Use WebDAV to mount ModeShape repositories as file servers
- Visual repository explorer web application


All of the JCR 2.0 features previously supported in 2.x are currently supported:

### Accessing the Repository
- RepositoryFactory access
- JNDI registration of Repository
- JAAS Authentication
- Servlet Authentication
- Custom Authentication

### Namespaces
- Session Remapping
- Permanent Addition/Deletion

### Reading Repository Content
- Traversal Access
- Direct Access
- Same-Name Siblings
- Multi-Value Properties
- All Property Types Supported
- Property Type Conversion

### Writing Repository Content
- Create/Update/Delete Nodes
- Create/Update/Delete Properties (Through Parent Nodes)
- Moving, Copying, Cloning
- Adding/Removing Mixins
- Referential integrity enforcement

### Query / Search
- XPath
- JCR-SQL
- JCR-SQL2
- JCR-QOM
- Full-Text Search

### Importing/Exporting Repository Content
- System View Import/Export
- Document View Import/Export

### Node Types
- Inheritance Among Node Types
- Discovering available Node Types
- Discovering the Node Types of a Node
- Discovering the Definition of a Node Type
- Property Constraints
- Automatic Item Creation
- Predefined standard Node Types
- Custom Node Type Registration (CND-Based and and JCR 2.0 API Template-Based)

### Repository Metadata under System Node
- Permanent Namespace Mappings
- Node Types (Built-In and User-Registered)
- Active Locks

### Other JCR Optional Features
- Observation
- Locking
- Versioning
- Shareable nodes
- Access controls
- Even journal

### Content Storage Options
- In-memory (local, replicated, and distributed)
- Relational databases (via JDBC), including in-memory, file-based, or remote
- LevelDB
- File system
- Cassandra
- Cloud storage (e.g., Amazon's S3, Rackspace's Cloudfiles, or any other provider supported by JClouds)
- Remote Infinispan

### Binary Storage Options
- File system
- JDBC database
- Infinispan
- MongoDB
- Chained binary stores

ModeShape also has features that go beyond the JCR API:

### ModeShape Federation Connectors
- File system connector (read and write)
- Git repository connector (read-only)
- CMIS reposiotry connector (read and write, tech preview)
- JDBC metadata connector (read-only)

### ModeShape Sequencers
- Compact Node Definition (CND) Sequencer
- DDL Sequencer
- Image Sequencer
- Java Source Sequencer
- Java Class Sequencer
- MP3 Sequencer
- MS Office Sequencer
- Text Sequencers (Delimited and Fixed Width)
- XML Sequencer
- XML Schema Document (XSD) Sequencer
- Web Service Definition Lanaguage (WSDL) 1.1 Sequencer
- Zip File Sequencer (also WARs, JARs, and EARs)
- Teiid Relational Model Sequencer
- Teiid VDB Sequencer

### ModeShape Deployment/Access Models
- JNDI-Based Deployment
- Deploy as a subsystem in JBoss Wildfly, with access to repositories via @Resource injection
- Deploy to other containers using ModeShape's JCA adapter
- Access through two RESTful Services (the 2.x-compatible API and a new improved API)
- Access through WebDAV Service
- Access through CMIS Service
- Access through visual repository explorer web application
- Local and remote JDBC drivers for accessing ModeShape content through JDBC API and JCR-SQL2 queries
- Embedded (in Server or JEE Archive) Deployment
- JTA support, allowing Sessions to participate in XA and container-managed transactions 
- OSGi-Compatible Archives

### Other ModeShape features
- Repository-wide backup and restoration
- Explicitly-defined indexes
- Automatic MIME type detection of binary content
- Asynchronous sequencing operations, within completion notified via events


## Bug Fixes, Features, and other Issues

The following are the bugs, features and other issues that have been fixed in the &version; release:

