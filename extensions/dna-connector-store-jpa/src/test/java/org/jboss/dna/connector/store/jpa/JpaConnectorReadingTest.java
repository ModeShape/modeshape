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
package org.jboss.dna.connector.store.jpa;

import org.jboss.dna.common.statistic.Stopwatch;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.ReadableConnectorTest;

/**
 * @author Randall Hauch
 */
public class JpaConnectorReadingTest extends ReadableConnectorTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        // Set the connection properties to be an in-memory HSQL database ...
        JpaSource source = new JpaSource();
        source.setName("Test Repository");
        source.setDialect("org.hibernate.dialect.HSQLDialect");
        source.setDriverClassName("org.hsqldb.jdbcDriver");
        source.setUsername("sa");
        source.setPassword("");
        source.setUrl("jdbc:hsqldb:.");
        source.setMaximumConnectionsInPool(3);
        source.setMinimumConnectionsInPool(0);
        source.setNumberOfConnectionsToAcquireAsNeeded(1);
        source.setMaximumSizeOfStatementCache(100);
        source.setMaximumConnectionIdleTimeInSeconds(0);
        source.setLargeValueSizeInBytes(150);

        // Create a graph and look up the root node. We do this to initialize the connection pool and
        // force the database to be setup at this point. By doing it now, we don't include this overhead
        // in our test timings.
        try {
            Graph graph = Graph.create(source, context);
            graph.getNodeAt("/");
        } catch (Throwable t) {
            Logger.getLogger(getClass()).debug("Unable to read the root node while setting up the \"" + source.getName()
                                               + "\" JPA source");
        }

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
        String initialPath = "";
        int depth = 4;
        int numChildrenPerNode = 4;
        int numPropertiesPerNode = 7;
        Stopwatch sw = new Stopwatch();
        boolean batch = true;
        graph.createWorkspace().named("default");
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);
        graph.createWorkspace().named("other workspace");
        createSubgraph(graph, initialPath, depth, numChildrenPerNode, numPropertiesPerNode, batch, sw, System.out, null);
        graph.useWorkspace("default");
    }
}
