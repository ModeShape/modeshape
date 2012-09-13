# Release Notes for ModeShape &version;

The ModeShape &version; release is the fourth beta release of our new architecture, and is suitable
only for testing and previewing features. APIs and storage formats are still subject to change.

## What's new

&version; provides a feature complete implementation of the JCR 2.0 (JSR-283) specification. 
This release adds support for full-text search, text extraction, and shareable nodes, plus 
it fixes quite a few bugs and issues. We've also improved the way binary values (and related 
information such as MIME types and extract text) are handled and stored within the BinaryStore.
This release also provides kits for installing ModeShape as a service into JBoss AS7.1.1.Final
and AS7.2 (requires building from code, since it hasn't yet been released).

Overall, ModeShape 3.0 has changed a lot since ModeShape 2.8.x:

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
- Support for participation in JTA and XA transactions, allowing (container-managed or bean-managed)
EJBs and JCR clients that programmatically use XA transactions to commit the changes in the transactions.
- New monitoring API that allows accessing the history for over a dozen metrics.
- New sequencing API, so sequencers now use the JCR API to get at the content being processed
and create/update the derived content. Sequencers can also dynamically register namespaces and
node types. Now it's easy to create custom sequencers.
- Simplified API for implementing custom MIME type detectors. ModeShape still has built-in
detectors that use the filename extensions and the binary content.
- New and simpler API for implementing custom text extractors.
- Improved storage of binary values of all sizes, with a separate facility for storing these on the file
system, in Infinispan caches, in relational DBMSes (via JDBC), and in MongoDB.
- API interfaces and methods that were deprecated in 2.7.0.Final (or later) have been removed.
There weren't many of these; most of the ModeShape API remains the same.
- Integration with JBoss AS 7. ModeShape runs as an integrated subsystem within AS7, and
the AS7 tooling can be used to define and manage repositories independently of each other
while the server is running.
- Local and remote JDBC drivers for issuing JCR-SQL2 queries and getting database metadata via the JDBC API
- Many bug fixes and minor improvements

## Features

Most of the JCR features previously supported in 2.x are working and ready for testing. 
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
- Separate large binary storage on file system (database and Infinispan coming soon)

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
- Deploy as a subsystem in JBoss AS7, with RHQ/JON monitoring, @Resource injection
- Access through RESTful Service
- Access through WebDAV Service
- Local and remote JDBC drivers for accessing ModeShape content through JDBC API and JCR-SQL2 queries
- Embedded (in Server or JEE Archive) Deployment
- JTA support, allowing Sessions to participate in XA and container-managed transactions 
- OSGi-Compatible Archives

### Other ModeShape features
- Automatic MIME type detection of binary content
- Asynchronous sequencing operations, within completion notified via events

A few features not related to the JCR 2.0 API have been planned and are still not yet
implemented. The most important one is enabling a repository to access the content in 
external systems (e.g., file system, SVN, JDBC, JCR, etc.). This most likely will be pushed
to 3.1 so that we can focus on releasing 3.0 with full support for JCR 2.0.


## Bug Fixes, Features, and other Issues
The following are the bugs, features and other issues that have been fixed in this release:

