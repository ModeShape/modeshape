/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.test.integration.jackrabbit;

import static org.junit.Assert.assertNotNull;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.i18n.MockI18n;
import org.jboss.dna.common.statistic.HistogramTest;
import org.jboss.dna.common.statistic.Stopwatch;
import org.jboss.dna.common.util.FileUtil;
import org.jboss.dna.common.util.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * These tests are designed to stress Jackrabbit in a variety of ways. Each test are independent of each other, and therefore each
 * test sets up a brand-new repository.
 */
public class JackrabbitDerbyStressTest {

    public static final String TESTATA_PATH = "./src/test/resources/";
    public static final String JACKRABBIT_DATA_PATH = "./target/testdata/jackrabbittest/";
    public static final String REPOSITORY_DIRECTORY_PATH = JACKRABBIT_DATA_PATH + "repository";
    public static final String REPOSITORY_CONFIG_PATH = TESTATA_PATH + "jackrabbitDerbyTestRepositoryConfig.xml";
    public static final String DERBY_SYSTEM_HOME = JACKRABBIT_DATA_PATH + "/derby";
    public static final String USERNAME = "jsmith";
    public static final char[] PASSWORD = "secret".toCharArray();

    private Logger logger;
    private Repository repository;
    private Session session;
    private Stopwatch stopwatch = new Stopwatch();

    @Before
    public void beforeEach() throws Exception {
        // Clean up the test data ...
        FileUtil.delete(JACKRABBIT_DATA_PATH);

        // Set up Derby and the logger ...
        System.setProperty("derby.system.home", DERBY_SYSTEM_HOME);
        logger = Logger.getLogger(JackrabbitDerbyStressTest.class);

        // Set up the transient repository ...
        logger.info(MockI18n.passthrough, "Creating test repository for stress test and logging in with user " + USERNAME);
        this.repository = new TransientRepository(REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH);

        SimpleCredentials creds = new SimpleCredentials(USERNAME, PASSWORD);
        session = this.repository.login(creds);
        assertNotNull(session);
    }

    @After
    public void afterEach() {
        try {
            if (session != null) session.logout();
        } finally {
            session = null;
            // No matter what, clean up the test data ...
            FileUtil.delete(JACKRABBIT_DATA_PATH);
        }
    }

    @Test
    public void shouldCreate100NodesWithNoChildrenAndNoProperties() throws Exception {
        Node rootNode = this.session.getRootNode();
        rootNode.addNode("node"); // don't measure the first one
        for (int i = 0; i != 100; ++i) {
            stopwatch.start();
            rootNode.addNode("node" + i);
            stopwatch.stop();
        }
        rootNode.save();
        HistogramTest.writeHistogramToLog(logger,
                                          stopwatch.getHistogram(3).setBucketCount(50),
                                          80,
                                          "create 100 nodes with no children and no properties");
        this.logger.info(MockI18n.passthrough, stopwatch.toString());
    }

    @Test
    public void shouldCreate1000NodesWithNoChildrenAndNoProperties() throws Exception {
        Node rootNode = this.session.getRootNode();
        rootNode.addNode("node"); // don't measure the first one
        for (int i = 0; i != 1000; ++i) {
            stopwatch.start();
            rootNode.addNode("node" + i);
            stopwatch.stop();
        }
        rootNode.save();
        HistogramTest.writeHistogramToLog(logger,
                                          stopwatch.getHistogram(1).setBucketCount(50),
                                          80,
                                          "create 1000 nodes with no children and no properties");
        this.logger.info(MockI18n.passthrough, stopwatch.toString());
    }

    @Test
    public void shouldCreate5000NodesWithNoChildrenAndNoProperties() throws Exception {
        Node rootNode = this.session.getRootNode();
        rootNode.addNode("node"); // don't measure the first one
        for (int i = 0; i != 5000; ++i) {
            stopwatch.start();
            rootNode.addNode("node" + i);
            stopwatch.stop();
        }
        rootNode.save();
        HistogramTest.writeHistogramToLog(logger,
                                          stopwatch.getHistogram(1).setBucketCount(50),
                                          80,
                                          "create 5000 nodes with no children and no properties");
        this.logger.info(MockI18n.passthrough, stopwatch.toString());
    }

    @Test
    public void shouldCreate10000NodesWithNoChildrenAndNoProperties() throws Exception {
        Node rootNode = this.session.getRootNode();
        rootNode.addNode("node"); // don't measure the first one
        for (int i = 0; i != 10000; ++i) {
            stopwatch.start();
            rootNode.addNode("node" + i);
            stopwatch.stop();
        }
        rootNode.save();
        HistogramTest.writeHistogramToLog(logger,
                                          stopwatch.getHistogram(1).setBucketCount(50),
                                          80,
                                          "create 10000 nodes with no children and no properties");
        this.logger.info(MockI18n.passthrough, stopwatch.toString());
    }

    @Test
    public void shouldCreate100NodesWithNoChildrenAndSeveralProperties() throws Exception {
        Node rootNode = this.session.getRootNode();
        rootNode.addNode("node"); // don't measure the first one
        for (int i = 0; i != 100; ++i) {
            stopwatch.start();
            Node child = rootNode.addNode("node" + i);
            child.setProperty("jcr:name", "This is the name of node " + i);
            child.setProperty("jcr:description", "This is the description of node " + i);
            stopwatch.stop();
        }
        rootNode.save();
        HistogramTest.writeHistogramToLog(logger,
                                          stopwatch.getHistogram(3).setBucketCount(50),
                                          80,
                                          "create 100 nodes with no children and several properties");
        this.logger.info(MockI18n.passthrough, stopwatch.toString());
    }
    //	
    // @Test
    // public void shouldCreate10000NodesWithNoChildrenAndSeveralProperties() throws Exception {
    // Node rootNode = this.session.getRootNode();
    // rootNode.addNode("node"); // don't measure the first one
    // for(int i=0; i!=10000; ++i ) {
    // stopwatch.start();
    // Node child = rootNode.addNode("node" + i);
    // child.setProperty("jcr:name", "This is the name of node " + i );
    // child.setProperty("jcr:description", "This is the description of node " + i );
    // stopwatch.stop();
    // }
    // rootNode.save();
    // HistogramTest.writeHistogramToLog(logger, stopwatch.getHistogram(3).setBucketCount(50), 80, "create 10000 nodes with no
    // children and several properties");
    // this.logger.info( stopwatch.toString() );
    // }
    //	
    // // @Test
    // // public void shouldCreate50000NodesWithNoChildrenAndSeveralProperties() throws Exception {
    // // Node rootNode = this.session.getRootNode();
    // // rootNode.addNode("node"); // don't measure the first one
    // // for(int i=0; i!=50000; ++i ) {
    // // stopwatch.start();
    // // Node child = rootNode.addNode("node" + i);
    // // child.setProperty("jcr:name", "This is the name of node " + i );
    // // child.setProperty("jcr:description", "This is the description of node " + i );
    // // stopwatch.stop();
    // // }
    // // rootNode.save();
    // // HistogramTest.writeHistogramToLog(logger, stopwatch.getHistogram(3).setBucketCount(50), 80, "create 50000 nodes with no
    // children and several properties");
    // // this.logger.info( stopwatch.toString() );
    // // }
    //	
    // @Test
    // public void shouldCreate100000NodesWithNoChildrenAndSeveralProperties() throws Exception {
    // Node rootNode = this.session.getRootNode();
    // rootNode.addNode("node"); // don't measure the first one
    // int index = 0;
    // stopwatch.start();
    // for(int j=0; j!=100; ++j ) {
    // for(int i=0; i!=1000; ++i ) {
    // ++index;
    // // stopwatch.start();
    // Node child = rootNode.addNode("node" + index);
    // child.setProperty("jcr:name", "This is the name of node " + index );
    // child.setProperty("jcr:description", "This is the description of node " + index );
    // // stopwatch.stop();
    // }
    // rootNode.save();
    // }
    // stopwatch.stop();
    // HistogramTest.writeHistogramToLog(logger, stopwatch.getHistogram(3).setBucketCount(50), 80, "create 100000 nodes (in 100
    // batches) with no children and several properties");
    // this.logger.info( stopwatch.toString() );
    // }
}
