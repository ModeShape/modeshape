# Release Notes for ModeShape &version;

This document outlines the changes that were made in ModeShape &version;.
We hope you enjoy it!

## What's new

&version; provides a fast, elastic, distributed hierarchical database that clients
work with via the standard JCR 2.0 (JSR-283) API. ModeShape 3 is a major upgrade over 2.x
and offers significant improvements in performance and scalability, while retaining all of
ModeShape 2's JCR-related features. ModeShape 3 has complete integration with JBoss EAP 6.1,
allowing deployed components to simply lookup and use repositories managed by ModeShape's 
service.

This release addesses 54 issues, many of which are bug fixes in lots of areas. The release
improves even more the file system connector so that very large files (even dozens of GB)
can be effectively accessed (MODE-2061). It is also now possible for clients to programmatically
invoke sequencers against transient content, with the output of the sequencers remaining in
transient state that will be saved when the session is saved (MODE-1467). The ModeShape kit for
EAP now supports both EAP 6.1.0 and 6.1.1, and now supports EAP's domain mode (tech preview,
MODE-2026). We also introduced a new Repository Explorer web application that enables using 
your browser to visually explore the ModeShape content (MODE-1820). This web application is 
automatically included with the ModeShape kit for EAP and is available for use on other servers.


## Features

ModeShape &version; has these features:

- ModeShape uses Infinispan for all caching and storage, giving a powerful and flexible
foundation for creating JCR repositories that are fast, scalable, and highly available.
Infinispan offers a great deal of storage options (via cache loaders), but using Infinispan 
as a distributed, mulit-site, in-memory data grid provides incredible scalability and performance.
- Strongly consistent. ModeShape is atomic, consistent, isolated and durable (ACID), so writing
applications is very natural. Applications can even use JTA transactions.
- Fast. ModeShape 3 is just plain seriously fast, and performance is all-around
faster than 2.x - most operations are at least one if not several orders of magnitude faster!
We'll publish performance and benchmarking results soon.
- Larger content. ModeShape 3 has been designed to store and access the content so that
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
- BerkleyDB
- Relational databases (via JDBC), including in-memory, file-based, or remote
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
- Automatic MIME type detection of binary content
- Asynchronous sequencing operations, within completion notified via events


## Bug Fixes, Features, and other Issues

The following are the bugs, features and other issues that have been fixed in the &version; release:

