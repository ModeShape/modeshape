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
package org.modeshape.jcr;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.naming.NamingException;
import org.hibernate.annotations.common.AssertionFailure;
import org.infinispan.config.Configuration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.Before;

/**
 * A base class for tests that require a new JcrSession and JcrRepository for each test method.
 */
public abstract class SingleUseAbstractTest {

    private static final String REPO_NAME = "testRepo";

    private CacheContainer cm;
    private RepositoryConfiguration config;
    protected JcrRepository repository;
    protected JcrSession session;

    @Before
    public void beforeEach() throws Exception {
        Configuration c = new Configuration();
        c = c.fluent().transaction().transactionManagerLookup(new DummyTransactionManagerLookup()).build();
        cm = TestCacheManagerFactory.createCacheManager(c);

        // Configuration c = new Configuration();
        // cm = TestCacheManagerFactory.createCacheManager(c, true);
        config = new RepositoryConfiguration(REPO_NAME, cm);
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
    }

    @After
    public void afterEach() throws Exception {
        try {
            repository.shutdown().get(3L, TimeUnit.SECONDS);
        } finally {
            repository = null;
            config = null;
            try {
                TestingUtil.killCacheManagers(cm);
            } finally {
                cm = null;
            }
        }
    }

    protected void predefineWorkspace( String workspaceName ) {
        // Edit the configuration ...
        Editor editor = config.edit();
        EditableDocument workspaces = editor.getOrCreateDocument("workspaces");
        EditableArray predefined = workspaces.getOrCreateArray("predefined");
        predefined.addStringIfAbsent(workspaceName);

        // And apply the changes ...
        Changes changes = editor.getChanges();
        if (changes.isEmpty()) return;
        try {
            repository.apply(changes);
        } catch (NamingException e) {
            throw new AssertionFailure("Unexpected error while predefining the \"" + workspaceName + "\" workspace", e);
        } catch (IOException e) {
            throw new AssertionFailure("Unexpected error while predefining the \"" + workspaceName + "\" workspace", e);
        }
    }

}
