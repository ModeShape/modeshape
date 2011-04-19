# Release Notes for ModeShape &version;

ModeShape &version; includes several new minor improvements and changes since 2.5.0.Beta1, including:
- upgrades of several third-party dependencies
- smaller JAR files (no longer dependent upon Google Collections or the JCIP libraries)
- versioning-related fixes and performance improvements
- about twenty bug fixes

The 2.5.0.Beta1 release included:
- new javax.jcr.Credentials classes for anonymous and HTTP servlet authentication
- a built-in profiling framework enabling use of Byteman runtime bytecode injection
- refactored the build process, which now uses Maven 3
- switch to PicketBox for the JAAS implementation used in unit tests
- performance improvements
- almost two dozen bug fixes

## JCR Supported Features

**ModeShape implements all of the required JCR 2.0 features** (repository acquisition, 
authentication, reading/navigating, query, export, node type discovery, and permissions and capability 
checking) and **most of the optional JCR 2.0 features** (writing, import, observation, workspace management
versioning, locking, node type management, same-name siblings, shareable nodes, and orderable 
child nodes). The remaining optional features in JCR 2.0 (access control management, 
lifecycle management, retention and hold, and transactions) may be introduced in future versions.

ModeShape supports the [JCR-SQL2][1] and [JCR-QOM][2] query languages defined in [JSR-283][3], plus the [XPath][4] and 
[JCR-SQL][5] languages defined in [JSR-170][6] but deprecated in JSR-283. ModeShape also supports a simple
[search-engine-like language][7] that is actually just the [full-text search expression grammar][8] 
used in the second parameter of the CONTAINS(...) function of the JCR-SQL2 language.

  [1]:  http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jcr-sql2-query-language
  [2]:  http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jcr-qom-query-language
  [3]:  http://jcp.org/en/jsr/detail?id=283
  [4]:  http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jcr-xpath-query-language
  [5]:  http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jcr-sql-query-language
  [6]:  http://jcp.org/en/jsr/detail?id=170
  [7]:  http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#fulltext-search-query-language
  [8]:  http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jcr-sql2-full-text-search-constraints

The &version; release has not yet been certified to be fully compliant with JCR 2.0. The ModeShape 
project plans to focus on attaining this certification in the very near future.

### Accessing the Repository
- JAAS Authentication
- HTTP Authentication (for RESTful and WebDAV Services Only)
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
- Locking
- Observation
- Versioning
- Shareable Nodes


## Connectors, Sequencers, and Other Features

As with previous releases, ModeShape &version; integrates with [JAAS][9], [web application security][10],
or you can easily [integrate it with other systems][11]. ModeShape can use a variety of back-ends to store 
information ([RDBMSes][12], [Infinispan data grid][13], [memory][14], [JBoss Cache][15], [JCR repositories][16]), can access content
in multiple systems ([file systems][17], [SVN repositories][18], [JDBC metadata][19]), can [federate][20] multiple stores and
systems into a single JCR repository, or can access other systems using [custom connectors][21].
ModeShape is also able to automatically extract and store useful content from files you upload into 
the repository using its library of [sequencers][22], making that information much more accessible and 
searchable than if it remains locked up inside the stored files. And ModeShape provides
[WebDAV and RESTful services][23] to allow various clients to access the content. For details, see the [Reference Guide][24].

  [9]:  http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jcr-sessions-jaas
  [10]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jcr-sessions-servlet
  [11]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jcr-sessions-custom
  [12]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jdbc-storage-connector
  [13]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#infinispan-connector
  [14]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#in-memory-connector
  [15]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jboss-cache-connector
  [16]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jcr-connector
  [17]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#file-system-connector
  [18]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#subversion-connector
  [19]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#jdbc-metadata-connector
  [20]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#federation-connector
  [21]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#custom-connectors
  [22]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#sequencing_framework
  [23]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html#web-access
  [24]: http://docs.jboss.org/modeshape/latest/manuals/reference/html_single/reference-guide-en.html


### Connectors
- Federated Connector
- JPA Connector (read-write/persistent storage)
- In-Memory Connector (read-write)
- JCR Connector (read-write)
- Infinispan Connector (read-write/persistent storage)
- JBoss Cache Connector (read-write/persistent storage)
- File System Connector (read-write/persistent storage of files & folders)
- Subversion Connector (read-write/persistent storage of files & folders)
- JDBC Metadata Connector (read-only)

### Sequencers
- Compact Node Definition (CND) Sequencer
- DDL Sequencer
- Image Sequencer
- Java Source Sequencer
- Java Class Sequencer
- MP3 Sequencer
- MS Office Sequencer
- Text Sequencers (Delimited and Fixed Width)
- XML Sequencer
- Zip File Sequencer (also WARs, JARs, and EARs)
- Teiid Relational Model Sequencer
- Teiid VDB Sequencer

### Deployment/Access Models
- Clustering
- JNDI-Based Deployment
- Embedded (in Server or JEE Archive) Deployment
- OSGi-Compatible Archives
- Access through RESTful Service
- Access through WebDAV Service
- Deploy as a service in JBoss Application Server, with JOPR monitoring
- JDBC driver for accessing ModeShape content through JDBC API and JCR-SQL2 queries

## Bug Fixes, Features, and other Issues
            