/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
/**
 * ModeShape uses <i>connectors</i> to access information from external systems (such as databases, 
 * other repositories, services, applications, etc.) and create graph representations of that information.
 * This package defines the interfaces that a connector must implement.
 * 
 * <h3>Concepts</h3>
 * <p>
 * A <strong>connector</strong> is the runnable code packaged in one or more JAR files that contains implementations
 * of several interfaces (described below). A Java developer writes a connector to a type of source, 
 * such as a particular database management system, LDAP directory, source code management system, etc. 
 * It is then packaged into one or more JAR files (including dependent JARs) and deployed for use in 
 * applications that use ModeShape repositories.
 * </p>
 * <p>
 * The description of a particular source system (e.g., the "Customer" database, or the company LDAP system) is 
 * called a <strong>repository source</strong>. ModeShape defines a {@link RepositorySource} interface that defines methods 
 * describing the behavior and supported features and a method for establishing connections. A connector will 
 * have a class that implements this interface and that has JavaBean properties for all of the connector-specific 
 * properties required to fully describe an instance of the system. Use of JavaBean properties is not required, 
 * but it is highly recommended, as it enables reflective configuration and administration. Applications that 
 * use ModeShape create an instance of the connector's {@link RepositorySource} implementation and set the properties 
 * for the external source that the application wants to access with that connector.
 * </p>
 * <p>
 * A repository source instance is then used to establish <strong>connections</strong> to that source. 
 * A connector provides an implementation of the {@link RepositoryConnection} interface, which defines methods for 
 * interacting with the external system. In particular, the execute(...) method takes an {@link org.modeshape.graph.ExecutionContext} 
 * instance and a {@link org.modeshape.graph.request.Request} object. The object defines the environment in which 
 * the processing is occurring, including information about the JAAS Subject and LoginContext. The 
 * {@link org.modeshape.graph.request.Request} object describes the requested 
 * operations on the content, with different concrete subclasses representing each type of activity. 
 * Examples of commands include (but not limited to) getting a node, moving a node, creating a node, 
 * changing a node, and deleting a node. And, if the repository source is able to participate in JTA/JTS 
 * distributed transactions, then the {@link RepositoryConnection} must implement the {@link RepositoryConnection#getXAResource()}
 * method by returning a valid {@link javax.transaction.xa.XAResource} object that can be used by the transaction monitor. 
 * </p>
 * 
 * <h3>Example connector</h3>
 * <p>
 * As an example, consider that we want ModeShape to give us access through JCR to the schema information contained
 * in a relational databases. We first have to develop a connector that allows us to interact with relational
 * databases using JDBC. That connector would contain a <code>JdbcRepositorySource</code> Java class that 
 * implements {@link RepositorySource}, and that has all of the various JavaBean properties for setting the 
 * name of the driver class, URL, username, password, and other properties. (Or we might have a JavaBean property 
 * that defines the JNDI name where we can find a JDBC DataSource instance pointing to our JDBC database.)
 * </p>
 * <p>
 * Our new connector would also have a <code>JdbcRepositoryConnection</code> Java class that implements the 
 * {@link RepositoryConnection} interface. This class would probably wrap a {@link java.sql.Connection JDBC database connection}, 
 * and would implement the {@link RepositoryConnection#execute(org.modeshape.graph.ExecutionContext, org.modeshape.graph.request.Request)}
 * method such that the nodes exposed by the connector describe the database schema of the database. For example, 
 * the connector might represent each database table as a node with the table's name, with properties that describe 
 * the table (e.g., the description, whether it's a temporary table), and with child nodes that represent each of 
 * the columns, keys and constraints.
 * </p>
 * <p>
 * To use our connector in an application that uses ModeShape, we need to create an instance of the 
 * <code>JdbcRepositorySource</code> for each database instance that we want to access. If we have 3 MySQL databases, 
 * 9 Oracle databases, and 4 PostgreSQL databases, then we'd need to create a total of 16 <code>JdbcRepositorySource</code> instances, 
 * each with the properties describing a single database instance. Those sources are then available for use by 
 * ModeShape components, including the JCR implementation.
 * </p>
 * 
 * <h3>Implementing a connector</h3>
 * <p>
 * As mentioned earlier, a connector consists of the Java code that is used to access content from a system. 
 * Perhaps the most important class that makes up a connector is the implementation of the {@link RepositorySource}. 
 * This class is analogous to JDBC's {@link javax.sql.DataSource} in that it is instantiated to represent a single 
 * instance of a system that will be accessed, and it contains enough information (in the form of JavaBean properties) 
 * so that it can create connections to the source.
 * </p>
 * <p>
 * Why is the RepositorySource implementation a JavaBean? Well, this is the class that is instantiated, usually reflectively, 
 * and so a no-arg constructor is required. Using JavaBean properties makes it possible to reflect upon the object's 
 * class to determine the properties that can be set (using setters) and read (using getters). This means that an 
 * administrative application can instantiate, configure, and manage the objects that represent the actual sources, 
 * without having to know anything about the actual implementation.
 * </p>
 * 
 * <h3>Testing a connector implementation</h3>
 * <p>
 * Testing connectors is not really that much different than testing other classes. Using mocks may help to isolate 
 * your instances so you can create more unit tests that don't require the underlying source system.
 * </p>
 * <p>
 * ModeShape does provide a set of {@link org.modeshape.graph.connector.test unit tests} that you can use to
 * verify that your connector "behaves correctly".  These are useful because you only have to set up the test case
 * classes (by extending one of the provided test case classes and overriding the appropriate set up methods),
 * but you don't have to write any test methods (since they're all inherited).
 * </p>
 * <p>
 * However, there may be times when you have to use the underlying source system in your tests. If this is the case, 
 * we recommend using Maven integration tests, which run at a different point in the Maven lifecycle. 
 * The benefit of using integration tests is that by convention they're able to rely upon external systems. 
 * Plus, your unit tests don't become polluted with slow-running tests that break if the external system is not available.
 * </p>
 */

package org.modeshape.graph.connector;

