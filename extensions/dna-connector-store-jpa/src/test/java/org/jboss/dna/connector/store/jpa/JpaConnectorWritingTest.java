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

import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.WritableConnectorTest;

/**
 * @author Randall Hauch
 */
public class JpaConnectorWritingTest extends WritableConnectorTest {

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
        source.setReferentialIntegrityEnforced(true);
        source.setLargeValueSizeInBytes(150);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
        graph.createWorkspace().named("default");
    }

    // @Test
    // public void shouldCreateFlatAndReallyWideTreeUsingOneBatch() {
    // String initialPath = "";
    // int depth = 1;
    // int numChildren = 10000;
    // int numBatches = 100;
    // int numChildrenPerNodePerBatch = numChildren / numBatches;
    // int numPropertiesPerNode = 7;
    // int total = 0;
    // Stopwatch sw = new Stopwatch();
    // System.out.println("" + numChildren + " children under a node");
    // for (int i = 0; i != numBatches; ++i) {
    // Graph.Batch batch = graph.batch();
    // total += createChildren(batch, initialPath, "node", numChildrenPerNodePerBatch, numPropertiesPerNode, depth, null);
    // Stopwatch sw2 = new Stopwatch();
    // sw.start();
    // sw2.start();
    // batch.execute();
    // sw.stop();
    // sw2.stop();
    // System.out.println(" ... created " + (total - numChildrenPerNodePerBatch + 1) + "-" + total + " in "
    // + sw2.getTotalDuration().getDuration(TimeUnit.MILLISECONDS) + " milliseconds");
    // }
    // System.out.println("  " + getTotalAndAverageDuration(sw));
    // assert total == numChildren;
    //
    // // Create 10 more ...
    // System.out.println(" Extra nodes (" + (total + 1) + "-" + (total + 10) + "):");
    // sw = new Stopwatch();
    // Graph.Batch batch = graph.batch();
    // createChildren(batch, initialPath, "added node", 10, numPropertiesPerNode, 1, null);
    // sw.start();
    // batch.execute();
    // sw.stop();
    // System.out.println("  " + getTotalAndAverageDuration(sw));
    // }
}
