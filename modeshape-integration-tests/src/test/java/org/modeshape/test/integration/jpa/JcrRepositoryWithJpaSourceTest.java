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
package org.modeshape.test.integration.jpa;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.net.URL;
import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.SecurityContext;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrSecurityContextCredentials;
import org.modeshape.test.integration.jpa.JcrRepositoryWithJpaConfigurationTest.CustomSecurityContext;

public class JcrRepositoryWithJpaSourceTest {

    private JcrEngine engine;
    private Repository repository;
    private Session session;
    private Credentials credentials;

    @Before
    public void beforeEach() throws Exception {
        final URL configUrl = getClass().getResource("/tck/simple-jpa/configRepository.xml");
        final String workspaceName = "otherWorkspace";
        assert configUrl != null;

        // Load the configuration from the config file ...
        JcrConfiguration config = new JcrConfiguration();
        config.loadFrom(configUrl);

        // Start the engine ...
        engine = config.build();
        engine.start();

        // Set up the fake credentials ...
        SecurityContext securityContext = new CustomSecurityContext("bill");
        credentials = new JcrSecurityContextCredentials(securityContext);

        repository = engine.getRepository("Test Repository Source");
        assert repository != null;
        session = repository.login(credentials, workspaceName);
        assert session != null;
    }

    @After
    public void afterEach() throws Exception {
        if (engine != null) {
            try {
                if (session != null) {
                    try {
                        session.logout();
                    } finally {
                        session = null;
                    }
                }
            } finally {
                repository = null;
                try {
                    engine.shutdown();
                } finally {
                    engine = null;
                }
            }
        }
    }

    // @Test
    // public void shouldHaveSession() {
    // assertThat(session, is(notNullValue()));
    // }

    @Test
    public void shouldBeAbleToRemoveNodeThatExists_Mode691() throws RepositoryException {
        // Create some content ...
        Node root = session.getRootNode();
        Node a = root.addNode("a");
        Node b = a.addNode("b");
        Node c = b.addNode("c");
        @SuppressWarnings( "unused" )
        Node d1 = c.addNode("d_one");
        @SuppressWarnings( "unused" )
        Node d2 = c.addNode("d_two");
        session.save();

        root = session.getRootNode();
        String pathToNode = "a/b";
        assertThat(root.hasNode(pathToNode), is(true));

        Node nodeToDelete = root.getNode(pathToNode);
        nodeToDelete.remove();
        session.save();

        root = session.getRootNode();
        assertThat(root.hasNode(pathToNode), is(false));
    }

}
