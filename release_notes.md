# Release Notes for ModeShape &version;

The ModeShape &version; release is the first patch release for the 
second stable release of our new architecture.
It's been a long journey with fourteen different alphas, betas, and candidate releases.
But this is a huge improvement over the 2.x series. We hope you enjoy it!

## What's new

&version; provides a fast, distributed, hierarchical database that clients
work with via the standard JCR 2.0 (JSR-283) API. ModeShape 3 is a major upgrade over 2.x
and offers significant improvements in performance and scalability, while retaining all of
ModeShape 2's JCR-related features. ModeShape 3 has complete integration with JBoss AS 7.1,
allowing deployed components to simply lookup and use repositories managed by ModeShape's 
service.

This release is a bug-fix release that includes 14 fixes (see below for details).


## Features

ModeShape &version; has changed a lot since ModeShape 2.8.x:

- ModeShape uses Infinispan for all caching and storage, giving a powerful and flexible
foundation for creating JCR repositories that are fast, scalable, and highly available.
Infinispan offers a great deal of storage options (via cache loaders), but using Infinispan 
as a distributed, mulit-site, in-memory data grid provides incredible scalability and performance.
- Improved performance. ModeShape 3 is just plain seriously fast, and performance is all-around
faster than 2.x - most operations are at least one if not several orders of magnitude faster!
We'll publish performance and benchmarking results soon.
- Improved scalability. ModeShape 3 has been designed to store and access the content so that
a node can have hundreds of thousands (or more!) of child nodes (even with same-name-siblings)
yet still be incredibly fast. Additionally, repositories can scale to many millions of nodes 
and be deployed across many processes.
- Improved configuration. There is no more global configuration of the engine; instead,
each repository is configured with a separate JSON file, which must conform to a ModeShape-specific
JSON Schema and can be validated by ModeShape prior to use. Repository configurations can even be
changed while the repository is running (some restrictions apply), making it possible to 
add/change/remove sequencers, authorization providers, and many other configuration options
while the repository is in use.
- Each repository can be deployed, started, stopped, and undeployed while the engine and other
repositories are still in use.
- Sessions immediately see all changes persisted/committed by other sessions, although
transient changes made by the session always take precedence.
- Support for participation in JTA transactions, allowing (container-managed or bean-managed)
EJBs and JCR clients that programmatically use transactions to commit the changes in the transactions.
- Monitoring API with over a dozen metrics.
- Sequencing SPI that uses the JCR API to get at the content being processed and create/update the 
derived content. Sequencers can also dynamically register namespaces and node types. Now it's easy 
to create custom sequencers.
- Connector SPI that defines how external systems are accessed and optionally updated to project
the external information into the repository as regular nodes.
- Simplified API for implementing custom MIME type detectors. ModeShape still has a built-in
Tika-based detector that determines MIME types using the filename extensions and binary content.
- New and simpler API for implementing custom text extractors, which extracts from binary values
searchable text used in full-text searches and queries.
- Improved storage of binary values of all sizes, with a separate facility for storing these on the file
system, in Infinispan caches, in relational DBMSes (via JDBC), and in MongoDB. Custom stores are also
possible.
- Public API interfaces and methods that were deprecated in 2.7.0.Final (or later) have been removed.
There weren't many of these; most of the ModeShape API remains the same as 2.x.
- Integration with JBoss AS 7. ModeShape runs as an integrated subsystem within AS7, and
the AS7 tooling can be used to define and manage repositories independently of each other
and while the server is running.
- Local and remote JDBC drivers for issuing JCR-SQL2 queries and getting database metadata via the JDBC API


All of the JCR features previously supported in 2.x are working and ready for use. 
If any issues are found, please log a bug report in our JIRA.

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

### ModeShape Federation Connectors
- File system connector (read and write)
- Git repository connector (read-only)

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
- Teiid Relational Model Sequencer (coming soon)
- Teiid VDB Sequencer (coming soon)

### ModeShape Deployment/Access Models
- JNDI-Based Deployment
- Deploy as a subsystem in JBoss AS7, with access to repositories via @Resource injection
- Deploy to other containers using ModeShape's JCA adapter
- Access through two RESTful Services (the 2.x-compatible API and a new improved API)
- Access through WebDAV Service
- Access through CMIS Service
- Local and remote JDBC drivers for accessing ModeShape content through JDBC API and JCR-SQL2 queries
- Embedded (in Server or JEE Archive) Deployment
- JTA support, allowing Sessions to participate in XA and container-managed transactions 
- OSGi-Compatible Archives

### Other ModeShape features
- Repository-wide backup and restoration
- Automatic MIME type detection of binary content
- Asynchronous sequencing operations, within completion notified via events

One feature not related to the JCR 2.0 API is enabling a repository to access the content in 
external systems (e.g., file system, SVN, JDBC, JCR, etc.). This feature was in 2.x but
has been pushed to 3.1 (roughly 6 weeks after 3.0.0.Final) so that we could release 3.0 
earlier with full support for JCR 2.0.


## Bug Fixes, Features, and other Issues

The following are the bugs, features and other issues that have been fixed in the &version; release:

