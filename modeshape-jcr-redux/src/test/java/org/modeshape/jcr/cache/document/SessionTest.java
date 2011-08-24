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
package org.modeshape.jcr.cache.document;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.transaction.TransactionManager;
import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.Schematic;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.RepositoryConfiguration;

public class SessionTest {

    private final String REPO_NAME = "testRepo";

    private CacheContainer cm;
    private TransactionManager txnMgr;
    private RepositoryConfiguration config;
    private JcrEngine engine;
    private Repository repository;
    private Session session;

    @Before
    public void beforeEach() throws Exception {
        cleanUpFileSystem();

        GlobalConfiguration global = new GlobalConfiguration();
        global = global.fluent().serialization().addAdvancedExternalizer(Schematic.externalizers()).build();

        CacheLoaderConfig loaderConfig = getCacheLoaderConfiguration();
        Configuration c = new Configuration();
        if (loaderConfig != null) {
            c = c.fluent().loaders().addCacheLoader(loaderConfig).build();
        }
        c = c.fluent().transaction().transactionManagerLookup(new DummyTransactionManagerLookup()).build();
        cm = TestCacheManagerFactory.createCacheManager(global, c);
        txnMgr = TestingUtil.getTransactionManager(cm.getCache(REPO_NAME));
        config = new RepositoryConfiguration(REPO_NAME, cm);
        engine = new JcrEngine();
        engine.start();
        engine.deploy(config);
        repository = engine.startRepository(config.getName()).get();
        session = repository.login();
    }

    @After
    public void afterEach() throws Exception {
        try {
            engine.shutdown().get(3L, TimeUnit.SECONDS);
        } finally {
            engine = null;
            repository = null;
            config = null;
            try {
                TestingUtil.killTransaction(txnMgr);
            } finally {
                try {
                    TestingUtil.killCacheManagers(cm);
                } finally {
                    cm = null;
                    cleanUpFileSystem();
                }
            }
        }
    }

    protected void cleanUpFileSystem() throws Exception {
        // do nothing by default
    }

    protected CacheLoaderConfig getCacheLoaderConfiguration() {
        return null;
    }

    @Test
    public void shouldHaveRootNode() throws Exception {
        Node node = session.getRootNode();
        assertThat(node, is(notNullValue()));
        assertThat(node.getPath(), is("/"));
    }

    @Test
    public void shouldHaveJcrSystemNodeUnderRoot() throws Exception {
        Node node = session.getRootNode();
        Node system = node.getNode("jcr:system");
        assertThat(system, is(notNullValue()));
        assertThat(system.getPath(), is("/jcr:system"));
    }

    @Test
    public void shouldAllowCreatingNodeUnderUnsavedNode() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        node.addNode("childNode");
        session.save();
    }

    @Test
    public void shouldAllowCreatingNodesTwoLevelsBelowRoot() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        session.save();
        node.addNode("childNode");
        session.save();
    }

    @Test
    public void shouldAllowDeletingNodeWithNoChildren() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        session.save();
        // session.getRootNode().getNodes();
        // System.out.println("Root: " + session.getRootNode().getNodes().getSize() + " children");
        node.remove();
        session.save();
    }

    @Test
    public void shouldAllowDeletingTransientNodeWithNoChildren() throws Exception {
        Node node = session.getRootNode().addNode("testNode");
        node.remove();
        session.save();
    }
}
