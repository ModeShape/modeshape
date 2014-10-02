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

ModeShape &version; is licensed under the Apache Software License, 2.0.

This version incorporates many new features and bug fixes. Some of these include:

* A new query engine with explicit index definitions and buffered results stored
off-heap that make it possible to handle very large results. All queries will work
whether or not indexes are defined, but without indexes they will be slow. Simply
look at the query plans for your queries, and define indexes that match your queries'
needs. When proper indexes are available, query execution is very fast. The query plan
will even show which indexes were considered for each part of the query. All of this is more
like how traditional databases are used, and it allows ModeShape to update only the indexes
(rather than indexing everything like in 3.x). Indexes can be defined as part of the configuration 
or programmatically via new API methods. New indexes are automatically populated 
with the repository contents, but this is an asynchronous process that make take
some time if the repository contents are large. Each index can be marked as updated
synchronously as part of the 'save' operation, or asynchronously in the background
after the 'save' call. Indexes are assigned to a single 'index provider'.
* An SPI for index providers, allowing customization of all indexing behavior.
A local index provider is included in 4.0, and it stores a complete copy of its indexes 
on each process in the cluster, making it very fast to query.
* More extensions to the JCR-SQL2 query language, including a new `mode:id` pseudocolumn
that provides access to exactly the same value as "Node.getIndentifier()" would via the 
API. There is also a new `CHILDCOUNT` dynamic operand that makes it very easy to find
nodes that have no children or to find nodes that have child counts within some range.
* New support for the JCR event journal feature, allowing applications to poll for changes
that occurred during specific time ranges. This is a useful alternative to listeners for 
operations may be expensive or time-consuming. Note that journaling is disabled by default.
* The internal event bus is vastly improved and substantially faster than in 3.x. 
Of course, there's no change in the event APIs so your listener implementations will 
continue to work unchanged. 
* The Repository Explorer web application was completely rewritten and is much more
dynamic. It's useful for developers of appliations that use the JCR API, allowing you
to visualize, navigate, and query repository content.
* Support for deploying ModeShape as a subsystem in Wildfly 8.x
* ModeShape now requires JDK 7. We don't expect any issues using Java 8, but let us know
if you have any problems.
* Clustering - ModeShape no longer has a clustering section in its configuration, since
we simply piggyback on top of Infinispan's clustering setup. So it's much easier to configure
clustering. We've also upgraded to a newer version of JGroups.
* Infinispan - We've moved to Infinispan 6.0.1.Final, which is faster and has new cache stores.
Some older and poorly-performaing cache stores are no longer valid, so check out the new
file-based cache stores. Also, the LevelDB cache store is supposedly very fast.



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

