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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.modeshape.common.junit.SkipLongRunning;
import org.modeshape.common.junit.SkipTestRule;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ModeShapeEngine.State;
import org.modeshape.jcr.cache.ChildReferences;

public class ConcurrentNodeLoadTest extends AbstractTransactionalTest {

    @Rule
    public TestRule skipTestRule = new SkipTestRule();

    private RepositoryConfiguration config;
    private ModeShapeEngine engine;
    private JcrRepository repository;
    private Session session;
    private boolean print;

    @Before
    public void beforeEach() throws Exception {
        FileUtil.delete("target/concurrent_load_non_clustered");
        print = false;
        config = RepositoryConfiguration.read("load/concurrent-load-repo-config.json");
        engine = new ModeShapeEngine();
    }

    @After
    public void afterEach() throws Exception {
        try {
            if (session != null) session.logout();
        } finally {
            session = null;
            try {
                engine.shutdown(true).get();
            } finally {
                repository = null;
                engine = null;
                config = null;
            }
        }
    }

    @Ignore
    @SkipLongRunning
    @Test
    public void shouldCreateLotsOfCustomersUnderSingleHierarchy() throws Exception {
        print = true;

        int totalNumberOfCustomers = 1000;

        int saveBatchSize = 20;
        int modifierThreadsCount = 500;
        int numberOfDecksPerCustomerAndThread = 4;

        startEngineAndDeployRepositoryAndLogIn();

        long start = System.nanoTime();
        // just one hierarchy /customers/aaa
        initializeDomainAndOneHierarchyNode("aaa");

        ExecutorService executorService = Executors.newFixedThreadPool(modifierThreadsCount);
        Set<String> allNames = new HashSet<String>(totalNumberOfCustomers);
        try {
            List<Future<?>> threadResults = new ArrayList<Future<?>>();
            Set<String> namesPerBatch = new HashSet<String>();

            for (int i = 0; i != totalNumberOfCustomers; ++i) {
                // make sure the customer is always under /customers/aaa
                String customerName = "aaa" + UUID.randomUUID().toString().substring(3);
                assertTrue("Duplicate customer generated", namesPerBatch.add(customerName));
                createCustomer(customerName);
                if (i >= saveBatchSize && i % saveBatchSize == 0) {
                    print("Saving  batch " + i);
                    session.save();
                    // fire up the threads; each thread will modify each customer and add numberOfDecksPerCustomerAndThread
                    for (int j = 0; j < modifierThreadsCount; j++) {
                        threadResults.add(executorService.submit(new CustomerModifier(new ArrayList<String>(namesPerBatch),
                                                                                      numberOfDecksPerCustomerAndThread)));
                    }
                    allNames.addAll(namesPerBatch);
                    namesPerBatch.clear();
                }
            }
            print("Saving final batch");
            session.save();
            if (!namesPerBatch.isEmpty()) {
                threadResults.add(executorService.submit(new CustomerModifier(new ArrayList<String>(namesPerBatch),
                                                                              numberOfDecksPerCustomerAndThread)));
            }
            print("Total time to insert records=" + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)
                  + " seconds with batch size=" + saveBatchSize);

            print("Waiting for " + threadResults.size() + " threads to complete");
            for (Future<?> future : threadResults) {
                future.get(1, TimeUnit.MINUTES);
            }
        } finally {
            executorService.shutdownNow();
        }

        start = System.nanoTime();
        assertUniqueChildren((JcrSession)session, "/customers/aaa", allNames);
        print("Total time to verify records=" + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) + " seconds");
    }

    private void assertUniqueChildren( JcrSession session,
                                       String nodeAbsPath,
                                       Set<String> names ) throws RepositoryException {
        ChildReferences childReferences = session.getNode(nodeAbsPath).node().getChildReferences(session.cache());
        for (String name : names) {
            assertEquals(1, childReferences.getChildCount(session.nameFactory().create(name)));
        }
    }

    protected void startEngineAndDeployRepositoryAndLogIn() throws Exception {

        print("starting engine");
        engine.start();
        assertThat(engine.getState(), is(State.RUNNING));

        print("deploying repository");
        repository = engine.deploy(config);
        session = repository.login();
        assertThat(session.getRootNode(), is(notNullValue()));
    }

    protected void initializeDomainAndOneHierarchyNode( String name ) throws Exception {
        Node domain = getOrCreate("customers", "acme:Domain", session.getRootNode());
        getOrCreate(name, "acme:Hierarchy", domain);
        session.save();
    }

    protected void initializeDomainAndHierarchyNodes() throws Exception {
        Node domain = getOrCreate("customers", "acme:Domain", session.getRootNode());
        for (int i = 0; i != 16; ++i) {
            final char c1 = Character.forDigit(i, 16);
            print("Adding hierarchy nodes under " + c1);
            for (int j = 0; j != 16; ++j) {
                final char c2 = Character.forDigit(j, 16);
                for (int k = 0; k != 16; ++k) {
                    final char c3 = Character.forDigit(k, 16);
                    String name = "" + c1 + c2 + c3;
                    getOrCreate(name, "acme:Hierarchy", domain);
                    // print("Adding node " + name);
                    // domain.addNode(name, );
                }
            }
            session.save();
        }
    }

    private final class CustomerModifier implements Callable<Void> {

        private final List<String> customerNames;
        private final int numberOfDecks;

        protected CustomerModifier( List<String> customerNames,
                                    int numberOfDecks ) {
            this.customerNames = customerNames;
            this.numberOfDecks = numberOfDecks;
        }

        @Override
        public Void call() throws Exception {
            JcrSession jcrSession = repository.login();
            try {
                Node domain = getOrCreate("customers", "acme:Domain", jcrSession.getRootNode());
                Node hierarchy = getOrCreate("aaa", "acme:Hierarchy", domain);
                for (String name : customerNames) {
                    for (int i = 0; i < numberOfDecks; i++) {
                        addContentToCustomer(jcrSession, hierarchy.getPath() + "/" + name, UUID.randomUUID().toString());
                    }
                }
                jcrSession.save();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                jcrSession.logout();
            }
        }
    }

    protected void createCustomer( String name ) throws Exception {
        String hierarchySegment = name.substring(0, 3);
        Node domain = session.getRootNode().getNode("customers");
        Node hierarchyNode = domain.getNode(hierarchySegment);
        Node customer = getOrCreate(name, "acme:DomainIdentifier", hierarchyNode);
        customer.setProperty("acme:resourceType", "ResourceTest");
    }

    protected void addContentToCustomer( Session session,
                                         String pathToCustomer,
                                         String childName ) throws Exception {
        // Add a child under the node ...
        session.refresh(false);
        Node customer = session.getNode(pathToCustomer);
        Node deck = customer.addNode(childName, "acme:DeckClassType");
        assertThat(deck, is(notNullValue()));
    }

    protected Node getOrCreate( String name,
                                String nodeTypeName,
                                Node parent ) throws Exception {
        try {
            return parent.getNode(name);
        } catch (PathNotFoundException e) {
            return parent.addNode(name, nodeTypeName);
        }
    }

    protected void print( String msg ) {
        if (print) {
            System.out.println(msg);
        }
    }
}
