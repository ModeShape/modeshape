# Release Notes for ModeShape &version;

The ModeShape &version; release is the first release of our new architecture, and is suitable
only for testing and previewing features. APIs and storage formats are still subject to change.

## What's new

There is a lot that's changed since ModeShape 2.7, including:

- ModeShape now uses Infinispan for all caching and storage, giving a powerful and flexible
foundation for creating JCR repositories that are fast, scalable, and highly available.
Infinispan offers a great deal of storage options (via cache loaders), but can also be used
as a distributed, mulit-site, in-memory data grid.
- Improved performance. ModeShape 3 is just plain seriously fast, and performance is all-around
faster than 2.x - most operations are at least one if not several orders of magnitude faster!
We'll publish performance and benchmarking results closer to the final release.
- Improved scalability. ModeShape 3 has been designed to store and access the content so that
a node can have hundreds of thousands of nodes (even with same-name-siblings) yet still be
incredibly fast. Additionally, repositories can scale to millions of nodes and be deployed
across many processes. (Clustering is not yet supported in this release.)
- Improved configuration. There is no more global configuration of the engine; instead,
each repository is configured with a separate JSON file, which must conform to a JSON Schema
and can be validated by ModeShape prior to use. Repository configurations can even be
changed while the repository is running (some restrictions apply), making it possible to 
add/change/remove sequencers, authorization providers, and many other configuration options
while the repository is in use.
- Each repository can be deployed, started, stopped, and undeployed while the engine and other
repositories are still in use.
- Sessions now immediately see all changes persisted/committed by other sessions, although
transient changes of the session always take precedence.
- New monitoring API that allows accessing the history for over a dozen metrics.
- New sequencing API, so sequencers now use the JCR API to get at the content being processed
and create/update the derived content. Sequencers can also dynamically register namespaces and
node types. Now it's easy to create custom sequencers.
- Simplified API for implementing custom MIME type detectors. ModeShape still has built-in
detectors that use the filename extensions and the binary content.
- Improved storage of binary values of all sizes, with a separate facility for storing these on the file
system. Storage of binary values in Infinispan and DBMSes will be added in upcoming releases.
- API interfaces and methods that were deprecated in 2.7.0.Final (or later) have been removed.
There weren't many of these; most of the ModeShape API remains the same.
- Many bug fixes and minor improvements

There are also several major new features that are planned (but not yet available in this release):

- Deployment to JBoss AS7. This is not yet available in this release, but by the next alpha
release we'll have kits that install ModeShape as an AS7 service, allowing you to configure
and manage repositories using the AS7 tooling.
- JTA support is not yet working in this release but will be soon. It will allow
JCR Sessions to participate in XA and container-managed transactions.
- Map-reduce based operations for performing reporting and custom read-only operations in parallel
against the entire content of a repository. ModeShape will use this to enable validation of
repository content against the current set or a proposed set of node types, as well as
optimizing the storage format/layout of each node.

## Features

Most of the JCR features previously supported in 2.x are working
and ready for testing. If any issues are found, please log a bug report in our JIRA.

### Accessing the Repository
- RepositoryFactory access
- JNDI registration of Repository
- JAAS Authentication
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
- Locking
- Versioning

### ModeShape Storage Options
- In-memory
- BerkleyDB
- Relational databases (via JDBC), including in-memory, file-based, or remote
- File system
- Cassandra
- Cloud storage (e.g., Amazon's S3, Rackspace's Cloudfiles, or any other provider supported by JClouds)
- Remote Infinispan
- Separate large binary storage on file system

### ModeShape Sequencers
- Compact Node Definition (CND) Sequencer
- DDL Sequencer
- Image Sequencer
- Java Source Sequencer
- Java Class Sequencer
- MP3 Sequencer
- MS Office Sequencer
- Text Sequencers (Delimited and Fixed Width)
- XML Schema Document (XSD) Sequencer

### ModeShape Deployment/Access Models
- JNDI-Based Deployment
- Embedded (in Server or JEE Archive) Deployment
- Sequencers: CND, DDL, images, Java (source and class files), MP3, MS Office, Text, XSD
- MIME type detection

However, a number of features are **not** yet implemented. Please do not use these
features or report problems; many will be implemented and ready for testing
in the next release.

### Query / Search
- XPath
- JCR-SQL
- JCR-SQL2
- JCR-QOM
- Full-Text Search

### Other JCR Optional Features
- Observation
- Shareable Nodes

### ModeShape Storage Options
- Federate and access content in external systems (e.g., file system, SVN, JDBC, JCR, etc.)
- Separate large binary storage in Infinispan and DBMS

### ModeShape Sequencers
- XML Sequencer
- Web Service Definition Lanaguage (WSDL) 1.1 Sequencer
- Zip File Sequencer (also WARs, JARs, and EARs)
- Teiid Relational Model Sequencer
- Teiid VDB Sequencer

### ModeShape Deployment/Access Models
- Clustering and grids
- OSGi-Compatible Archives
- Access through RESTful Service
- Access through WebDAV Service
- JTA support, allowing Sessions to participate in XA and container-managed transactions 
- Deploy as a service in JBoss Application Server, with RHQ/JON monitoring
- JDBC driver for accessing ModeShape content through JDBC API and JCR-SQL2 queries


## Bug Fixes, Features, and other Issues
The following are the bugs, features and other issues that have been fixed in this release:

