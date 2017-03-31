# Release Notes for ModeShape &version;

This document outlines the changes that were made in ModeShape &version;.
We hope you enjoy it!

## What's new

This release addresses 19 bugs and 4 enhancements, most notably:
- adding support for pass-through configuration properties for the Hikari connection pool (see [MODE-2674](https://issues.jboss.org/browse/MODE-2674)) 

- fixing a number of transaction and clustering related issues 

- upgrading the Wildfly support to the Wildfly 10.1.0.Final 

- adding generic Amazon S3 support for binary storage

## Migrating from ModeShape 3 or ModeShape 4 

If you're planning on migrating from earlier versions of ModeShape to ModeShape 5 (which we strongly encourage) make sure you
read [the migration guide](https://docs.jboss.org/author/display/MODE50/Migrating+from+3.x+and+4.x)

## Starting with ModeShape for the first time

If you're starting to use ModeShape for the first time, make sure you read [the getting started guide](https://docs.jboss.org/author/display/MODE50/Getting+Started)

## What to test

Since this is a new major release, all features targeted to 5.0 are complete and
are suitable for testing.

We would like to get as much feedback as possible, so we do ask that our
community do testing with &version; to help us identify problems. Specifically,
we ask that you test the following areas:

* JDK - ModeShape now requires JDK 8. 
* Clustering - ModeShape 5 still supports clustering, but in a much more conservative fashion. See [our clustering documentation](https://docs.jboss.org/author/display/MODE50/Clustering)
* New persistence stores - We've moved away from Infinispan and now provide our own persistent stores, so we'd greatly appreciate
any feedback around the new persistence model

## Features

ModeShape &version; has these features:

- ModeShape provides its own persistence stores, focusing primarily on data integrity
- Strongly consistent. ModeShape is atomic, consistent, isolated and durable (ACID), so writing
applications is very natural. Applications can even use JTA transactions.
- Fast. ModeShape 5 should be just as fast in most cases as previous versions
- Larger content. ModeShape 5 can store and access the content so that
a node can have hundreds of thousands (or more!) of child nodes (even with same-name-siblings)
yet still be incredibly fast. Additionally, repositories can scale to many millions of nodes 
and be deployed across many processes.
- Simplified configuration. There is no more global configuration of the engine; instead,
each repository is configured with a separate JSON file, which must conform to a ModeShape-specific
JSON Schema and can be validated by ModeShape prior to use. Repository configurations can even be
changed while the repository is running (some restrictions apply), making it possible to 
add/change/remove sequencers, authorization providers, and many other configuration options
while the repository is in use.
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
system, in relational DBMSes (via JDBC), in MongoDB or in Cassandra. Custom stores are also
possible.
- Indexes to optimize query performance. ModeShape offers a number of different index providers and can store
and uses indexes in a variety of fashions
- Integration with JBoss Wildfly. ModeShape runs as an integrated subsystem within Wildfly, and
the Wildfly tooling can be used to define and manage repositories independently of each other
and while the server is running.
- Local and remote JDBC drivers for issuing JCR-SQL2 queries and getting database metadata via the JDBC API
- Use the RESTful API to talk to ModeShape repositories from non-Java and non-local applications
- Use the CMIS API to talk to ModeShape repositories
- Use WebDAV to mount ModeShape repositories as file servers
- Visual repository explorer web application


All of the JCR 2.0 features supported in previous versions are still supported:

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
- Full Versioning
- Shareable nodes
- Access controls
- Event journal

### Content Storage Options
- In-memory
- Relational databases (via JDBC), including in-memory, file-based, or remote
- File system

### Binary Storage Options
- File system
- JDBC database
- MongoDB
- Cassandra
- Chained binary stores

ModeShape also has features that go beyond the JCR API:

### ModeShape Federation Connectors
- File system connector (read and write)
- Git repository connector (read-only)
- CMIS reposiotry connector (read and write, tech preview)
- JDBC metadata connector (read-only)

### ModeShape Sequencers
- Audio Sequencer
- Compact Node Definition (CND) Sequencer
- DDL Sequencer
- EPUB Sequencer
- Image Sequencer
- Java Source Sequencer
- Java Class Sequencer
- MP3 Sequencer (deprecated)
- MS Office Sequencer
- ODF Sequencer
- PDF Sequencer
- Text Sequencers (Delimited and Fixed Width)
- Video Sequencer
- XML Sequencer
- XML Schema Document (XSD) Sequencer
- Web Service Definition Lanaguage (WSDL) 1.1 Sequencer
- Zip File Sequencer (also WARs, JARs, and EARs)

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

