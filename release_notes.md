# Release Notes for ModeShape &version;

This document outlines the changes that were made in ModeShape &version;.
We hope you enjoy it!

## What's new

&version; provides a fast, elastic, distributed hierarchical database that clients
work with via the standard JCR 2.0 (JSR-283) API. ModeShape 4 is a major upgrade over 3.x
and offers significant improvements in clustering, performance, query, and events.
All JCR 2.0 features are supported, and ModeShape 4 has complete integration with
Wildfly 8, allowing deployed applications to simply lookup and use repositories managed 
by ModeShape's service.

As of 4.0.0.Alpha1, ModeShape is licensed under the Apache Software License, 2.0.

This is the first beta release of the 4.0 stream, and it includes 46 bug fixes
and several new features. There is now support for explicit single-column indexes stored
locally on the file system. As a result, the index provider SPI first released in
an earlier alpha has changed slightly, though we expect it to remain unchanged
from this point forward. We've also extended the JCR-SQL2 query language with 
a new pseudocolumn, "mode:id", that provides access to exactly the same value
as "Node.getIndentifier()" would via the API. Like all pseudocolumns, it can be
used in WHERE constraints and JOIN criteria.

The first alpha release introduced a new query engine that allows 
clients to explicitly define the indexes used in the query system, and the second
alpha release brought minor changes to the Service Provider Interface (SPI) for 
query providers and introduced a programmatic API and configuration
modifications for defining indexes, although no complete query providers are included
(see next release). Alpha2 had support for the JCR event journal feature, allowing
applications to poll for changes that occurred during time ranges. This is a useful
alternative to listeners that may be expensive or time-consuming. Alpha3 introduced
a new event system and our new ring buffer that is substantially faster than what
we had in 3.x; of course, there's no change in the event APIs so your listener 
implementations will continue to work unchanged. Alpha4 fixed a number of issues
and introduced the newly redesigned Repository Explorer web application that
can be deployed in a web server alongside ModeShape, including on Wildfly 8.


## What to test

Since this is the first beta release, all features targeted to 4.0 are complete and
are suitable for testing. It is not a stable release, so please do not put &version;
into production.

We would like to get as much feedback as possible, so we do ask that our
community do testing with &version; to help us identify problems. Specifically,
we ask that you test the following areas:

* JDK - ModeShape now requires JDK 7. We've not yet begun testing with Java 8, but we'd
be happy to hear about it if you do.
* Queries - the new query engine passes all of our regression tests. Without explicit
indexes, all queries are expected to work properly but may be slow (except for very tiny
repositories). You can explicitly define indexes via the configuration files or programmatically
via ModeShape's public API. In fact, it is recommended that you define indexes that can
be used in each of your queries, and doing so will make those queries much faster.
The query plan contains information about all indexes considered by the query engine 
as well the index (if any) selected for use in each part of the query; please use the
query plan to understand whether your indexes are being used.
* Clustering - ModeShape no longer has a clustering section in its configuration, since
we simply piggyback on top of Infinispan's clustering setup. We've also upgraded to 
a newer version of JGroups.
* Journalling - Try enabling journaling and verify it works and does not affect performance.
Then try using the JCR Event Journal feature.
* Infinispan - We've moved to Infinispan 6.0.1.Final, which is faster and has new cache stores.
Some older and poorly-performaing cache stores are no longer valid, so check out the new
file-based cache stores. Also, the LevelDB cache store is supposedly very fast.
* Backup and restore - given that some older Infinispan cache stores are no longer supported,
in order to test migrating 3.x repositories to 4.0 you will need to use ModeShape's backup
and restore feature. If you don't regularly use that, please test it with your repository.
Just be sure not to overwrite any 3.x repositories.
* Bugs - we've fixed a number of bugs reported against and fixed in 3.x; see the list 
below for details. All these are ready for testing.
* Configuration - comments are now allowed in our JSON configuration.


## Features

ModeShape &version; has these features:

- ModeShape uses Infinispan for all caching and storage, giving a powerful and flexible
foundation for creating JCR repositories that are fast, scalable, and highly available.
Infinispan offers a great deal of storage options (via cache loaders), but using Infinispan 
as a distributed, mulit-site, in-memory data grid provides incredible scalability and performance.
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
- Integration with JBoss AS 7. ModeShape runs as an integrated subsystem within AS7, and
the AS7 tooling can be used to define and manage repositories independently of each other
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
- Deploy as a subsystem in JBoss AS7, with access to repositories via @Resource injection
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

