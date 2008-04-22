/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.tests.integration.jackrabbit;

import static org.junit.Assert.assertNotNull;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.stats.HistogramTest;
import org.jboss.dna.common.stats.Stopwatch;
import org.jboss.dna.common.util.FileUtil;
import org.jboss.dna.common.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * These tests are designed to stress Jackrabbit in a variety of ways, using the in-memory persistence manager. Each test are
 * independent of each other, and therefore each test sets up a brand-new repository.
 */
public class JackrabbitInMemoryTest {

    public static final String TESTATA_PATH = "./src/test/resources/";
    public static final String JACKRABBIT_DATA_PATH = "./target/testdata/jackrabbittest/";
    public static final String REPOSITORY_DIRECTORY_PATH = JACKRABBIT_DATA_PATH + "repository";
    public static final String REPOSITORY_CONFIG_PATH = TESTATA_PATH + "jackrabbitInMemoryTestRepositoryConfig.xml";
    public static final String USERNAME = "jsmith";
    public static final char[] PASSWORD = "secret".toCharArray();

    private Logger logger;
    private Repository repository;
    private Session session;
    private Stopwatch nodeStopwatch = new Stopwatch();
    private Stopwatch saveStopwatch = new Stopwatch();

    @Before
    public void beforeEach() throws Exception {
        // Clean up the test data ...
        FileUtil.delete(JACKRABBIT_DATA_PATH);

        // Set up the logger ...
        logger = Logger.getLogger(JackrabbitInMemoryTest.class);

        // Set up the transient repository ...
        logger.info(CommonI18n.passthrough, "Creating test repository for stress test and logging in with user " + USERNAME);
        this.repository = new TransientRepository(REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH);

        SimpleCredentials creds = new SimpleCredentials(USERNAME, PASSWORD);
        session = this.repository.login(creds);
        assertNotNull(session);
    }

    @After
    public void afterEach() throws Exception {
        try {
            if (session != null) session.logout();
        } finally {
            session = null;
            // No matter what, clean up the test data ...
            FileUtil.delete(JACKRABBIT_DATA_PATH);
        }
    }

    /**
     * Create an evenly distributed tree of nodes, starting with the supplied parent.
     * @param parentNode the parent node of the tree; may not be null
     * @param numberOfChildNodes the number of child nodes to create under the parent
     * @param levelsToCreate the total number of levels in the tree to create
     * @return the total number of child nodes created
     * @throws Exception
     */
    public int createNodes( Node parentNode, int numberOfChildNodes, int levelsToCreate ) throws Exception {
        int numberCreated = 0;
        for (int i = 0; i < numberOfChildNodes; ++i) {
            nodeStopwatch.start();
            Node child = parentNode.addNode("node" + i);
            child.setProperty("jcr:name", "This is the name of node " + i);
            child.setProperty("jcr:description", "This is the description of node " + i);
            nodeStopwatch.stop();
            // this.logger.debug(" - " + child.getPath());
            ++numberCreated;
            if (levelsToCreate > 1) {
                numberCreated += createNodes(child, numberOfChildNodes, levelsToCreate - 1);
            }
        }
        return numberCreated;
    }

    @Test
    public void shouldCreate10NodesWithNoChildrenAndNoProperties() throws Exception {
        Node rootNode = this.session.getRootNode();
        rootNode.addNode("node"); // don't measure the first one
        for (int i = 0; i != 10; ++i) {
            nodeStopwatch.start();
            rootNode.addNode("node" + i);
            nodeStopwatch.stop();
        }
        rootNode.save();
        HistogramTest.writeHistogramToLog(logger, nodeStopwatch.getHistogram(3).setBucketCount(50), 80, "create 100 nodes with no children and no properties");
        this.logger.info(CommonI18n.passthrough, nodeStopwatch.toString());
    }

    @Test
    public void shouldCreateTreeOfNodes2LevelsDeepWith10ChildrenAtEachNode() throws Exception {
        Node rootNode = this.session.getRootNode();
        Node parent = rootNode.addNode("node"); // don't measure the first one
        int numNodes = this.createNodes(parent, 10, 2);
        saveStopwatch.start();
        rootNode.save();
        saveStopwatch.stop();

        HistogramTest.writeHistogramToLog(logger, nodeStopwatch.getHistogram(3).setBucketCount(50), 80, "create tree of " + numNodes + " nodes (2 deep, 10 children at every node)");
        HistogramTest.writeHistogramToLog(logger, saveStopwatch.getHistogram(3).setBucketCount(50), 80, "1 save of 2x10 tree of " + numNodes + " nodes");
        this.logger.info(CommonI18n.passthrough, "Node operation times: " + nodeStopwatch.toString());
        this.logger.info(CommonI18n.passthrough, "Save times: " + saveStopwatch.toString());
    }

    @Test
    public void shouldExportSystemBranchToSystemView() throws Exception {
        this.session.exportSystemView("/jcr:system", System.out, true, false);
    }

    @Test
    public void shouldExportSystemBranchToDocumentView() throws Exception {
        this.session.exportDocumentView("/jcr:system", System.out, true, false);
    }

}
