/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.io.File;
import java.net.URI;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Collections;
import java.util.Properties;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.jackrabbit.test.JCRTestSuite;
import org.apache.jackrabbit.test.RepositoryStub;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.GraphImporter;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;

/**
 * Test suite to wrap Apache Jackrabbit JCR technology compatibility kit (TCK) unit tests.
 * 
 * @see JcrTckLevelOneTestSuite
 * @see JcrTckLevelTwoTestSuite
 * @see JcrTckOptionalTestSuite
 */
public class JcrTckTest {

    /**
     * Wrapper so that the Jackrabbit TCK test suite gets picked up by the DNA Maven test target.
     * 
     * @return a new instance of {@link JCRTestSuite}.
     */
    public static Test suite() {
        // Uncomment this to execute all tests
        // return new JCRTestSuite();

        // Or uncomment the following lines to execute the different sets/suites of tests ...
        TestSuite suite = new TestSuite("JCR 1.0 API tests");
        suite.addTest(JcrTckLevelOneTestSuite.suite());
        // suite.addTest(JcrTckLevelTwoTestSuite.suite());
        // suite.addTest(JcrTckOptionalTestSuite.suite());

        return suite;
    }

    /**
     * Concrete implementation of {@link RepositoryStub} based on DNA-specific configuration.
     */
    public static class InMemoryRepositoryStub extends RepositoryStub {
        private Repository repository;
        protected RepositoryConnection connection;
        protected AccessControlContext accessControlContext = AccessController.getContext();

        private Credentials credentials = new Credentials() {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public AccessControlContext getAccessControlContext() {
                return accessControlContext;
            }
        };

        protected ExecutionContext executionContext = new ExecutionContext() {

            @Override
            public ExecutionContext create( AccessControlContext accessControlContext ) {
                return executionContext;
            }
        };

        protected RepositoryConnectionFactory connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) {
                return connection;
            }
        };

        public InMemoryRepositoryStub( Properties env ) {
            super(env);

            // Create the in-memory (DNA) repository
            InMemoryRepositorySource source = new InMemoryRepositorySource();

            // Various calls will fail if you do not set a non-null name for the source
            source.setName("TestRepositorySource");

            // Wrap a connection to the in-memory (DNA) repository in a (JCR) repository
            connection = source.getConnection();
            repository = new JcrRepository(Collections.<String, String>emptyMap(), executionContext.create(accessControlContext),
                                           connectionFactory, source.getName());

            // Make sure the path to the namespaces exists ...
            Graph graph = Graph.create(source.getName(), connectionFactory, executionContext);
            graph.create("/jcr:system").and().create("/jcr:system/dna:namespaces");

            // Set up some sample nodes in the graph to match the expected test configuration
            try {

                // TODO: Should there be an easier way to define these since they will be needed for all JCR repositories?
                executionContext.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
                executionContext.getNamespaceRegistry().register(JcrLexicon.Namespace.PREFIX, JcrLexicon.Namespace.URI);
                executionContext.getNamespaceRegistry().register(JcrNtLexicon.Namespace.PREFIX, JcrNtLexicon.Namespace.URI);
                executionContext.getNamespaceRegistry().register("sv", "http://www.jcp.org/jcr/sv/1.0");

                Path destinationPath = executionContext.getValueFactories().getPathFactory().create("/");
                GraphImporter importer = new GraphImporter(graph);

                URI xmlContent = new File("src/test/resources/repositoryJackrabbitTck.xml").toURI();
                Graph.Batch batch = importer.importXml(xmlContent, Location.create(destinationPath));
                batch.execute();

            } catch (Exception ex) {
                // The TCK tries to quash this exception. Print it out to be more obvious.
                ex.printStackTrace();
                throw new IllegalStateException("Repository initialization failed.", ex);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.jackrabbit.test.RepositoryStub#getSuperuserCredentials()
         */
        @Override
        public Credentials getSuperuserCredentials() {
            // TODO: Why must we override this method? The default TCK implementation just returns a particular instance of
            // SimpleCredentials.
            return credentials;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.jackrabbit.test.RepositoryStub#getReadOnlyCredentials()
         */
        @Override
        public Credentials getReadOnlyCredentials() {
            // TODO: Why must we override this method? The default TCK implementation just returns a particular instance of
            // SimpleCredentials.
            return credentials;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.apache.jackrabbit.test.RepositoryStub#getRepository()
         */
        @Override
        public Repository getRepository() {
            return repository;
        }

    }

}
