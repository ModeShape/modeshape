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
package org.modeshape.test.integration.federation;

import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.collection.Problem;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;

public class FederationIspnConnectorRepositoryTest {

    private static JcrConfiguration configuration;
    private static JcrEngine engine;
    private static List<Session> sessions = new ArrayList<Session>();
    private boolean print = false;

    @BeforeClass
    public static void beforeAll() throws Exception {
        configuration = new JcrConfiguration();
        configuration.loadFrom("src/test/resources/config/configRepositoryForFederatedInfinispan.xml");

        // Create an engine and use it to populate the source ...
        engine = configuration.build();
        try {
            engine.start(true);
        } catch (RuntimeException e) {
            // There was a problem starting the engine ...
            System.err.println("There were problems starting the engine:");
            for (Problem problem : engine.getProblems()) {
                System.err.println(problem);
            }
            throw e;
        }
    }

    @AfterClass
    public static void afterAll() throws Exception {
        // Close all of the sessions ...
        for (Session session : sessions) {
            if (session.isLive()) session.logout();
        }
        sessions.clear();

        // Shut down the engines ...
        if (engine != null) {
            try {
                engine.shutdown();
            } finally {
                engine = null;
            }
        }
    }

    @Before
    public void beforeEach() {
        print = true;
    }

    void printNodes( Node node,
                     int indent ) throws RepositoryException {
        if (!print) return;

        NodeIterator i = node.getNodes();
        while (i.hasNext()) {
            Node child = i.nextNode();
            for (int j = 0; j < indent * 4; j++) {
                System.out.print(' ');
            }
            System.out.println(child);
            if (!"jcr:system".equals(child.getName())) {
                printNodes(child, indent + 1);
            }
        }
    }

    void print( String msg ) {
        if (print) System.out.println(msg);
    }

    @FixFor( "MODE-1256" )
    @Test
    public void shouldCreateContent() throws Throwable {

        // Add some content to the in-memory store ...
        Repository repository = engine.getRepository("test-inmemory");
        Session session = repository.login("default");
        sessions.add(session);

        print = false;
        session.getRootNode().addNode("a");
        session.getRootNode().addNode("a/b");
        session.save();
        printNodes(session.getRootNode(), 0);
        session.logout();

        // Add some content to the Infinispan store ...
        repository = engine.getRepository("test-infinispan");
        session = repository.login("default");
        sessions.add(session);

        session.getRootNode().addNode("x");
        session.getRootNode().addNode("x/y");
        session.getRootNode().addNode("x/z");
        session.save();
        printNodes(session.getRootNode(), 0);
        session.logout();

        // Now connect to the federated repository and verify some content ...
        repository = engine.getRepository("test-federated");
        session = repository.login("default");
        // print = true;
        print("\nBefore move");
        printNodes(session.getRootNode(), 0);
        session.getNode("/jcr:system");
        session.getNode("/inmemory"); // should be 'a'
        session.getNode("/inmemory/b");
        session.getNode("/infinispan"); // should be 'x'
        session.getNode("/infinispan/y");
        session.getNode("/infinispan/z");
        session.logout();

        // Now move a node ...
        session = repository.login("default");
        session.move("/infinispan/y", "/infinispan/newY");
        print("\nAfter move, before save");
        printNodes(session.getRootNode(), 0);
        Throwable unexpected = null;
        try {
            session.save();
            print("\nAfter move, after save");
            printNodes(session.getRootNode(), 0);
        } catch (Throwable t) {
            unexpected = t;
        } finally {
            session.logout();
        }

        // And create another node ...
        session = repository.login("default");
        session.getNode("/infinispan").addNode("newNode", "nt:unstructured");
        session.save();
        print("\nAfter adding '/infinispan/newNode' and saving");
        printNodes(session.getRootNode(), 0);
        session.logout();
        if (unexpected != null) throw unexpected;
    }
}
