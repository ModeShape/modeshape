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
package org.modeshape.test.integration;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.connector.store.jpa.JpaSource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.ModeShapeRoles;
import org.modeshape.jcr.JcrRepository.Option;

public class ClusteringTest {

    private static JcrConfiguration configuration;
    private static JcrEngine engine1;
    private static JcrEngine engine2;
    private static JcrEngine engine3;
    private static List<Session> sessions = new ArrayList<Session>();

    @BeforeClass
    public static void beforeEach() throws Exception {
        // Delete the database files, if there are any ...
        FileUtil.delete("target/db");

        // Set up the database and keep this connection open ...
        String url = "jdbc:hsqldb:file:target/db/ClusteredObservationBusTest";
        String username = "sa";
        String password = "";

        configuration = new JcrConfiguration();
        configuration.repositorySource("car-source")
                     .usingClass(JpaSource.class)
                     .setDescription("The automobile content")
                     .setProperty("dialect", "org.hibernate.dialect.HSQLDialect")
                     .setProperty("driverClassName", "org.hsqldb.jdbcDriver")
                     .setProperty("url", url)
                     .setProperty("username", username)
                     .setProperty("password", password)
                     .setProperty("maximumConnectionsInPool", "2")
                     .setProperty("minimumConnectionsInPool", "1")
                     .setProperty("numberOfConnectionsToAcquireAsNeeded", "1")
                     .setProperty("maximumSizeOfStatementCache", "100")
                     .setProperty("maximumConnectionIdleTimeInSeconds", "10")
                     .setProperty("referentialIntegrityEnforced", "true")
                     .setProperty("largeValueSizeInBytes", "150")
                     .setProperty("autoGenerateSchema", "update")
                     .setProperty("retryLimit", "3")
                     .setProperty("showSql", "false");
        configuration.repository("cars")
                     .setSource("car-source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES, ModeShapeRoles.ADMIN);
        configuration.clustering().setProperty("clusterName", "MyCluster");// .setProperty("configuration", "");

        // Create an engine and use it to populate the source ...
        engine1 = configuration.build();
        engine1.start();

        Repository repository = engine1.getRepository("cars");

        // Use a session to load the contents ...
        Session session = repository.login();
        try {
            InputStream stream = resourceStream("io/cars-system-view.xml");
            try {
                session.getWorkspace().importXML("/", stream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                stream.close();
            }
        } finally {
            session.logout();
        }

        // Make sure the data was imported ...
        session = repository.login();
        try {
            Node node = session.getRootNode().getNode("Cars/Hybrid/Toyota Highlander");
            assertThat(node, is(notNullValue()));
            assertThat(session.getRootNode().getNodes().getSize(), is(2L)); // "Cars" and "jcr:system"
        } finally {
            session.logout();
        }

        // Start the other engines ...
        engine2 = configuration.build();
        engine2.start();

        engine3 = configuration.build();
        engine3.start();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        // Close all of the sessions ...
        for (Session session : sessions) {
            if (session.isLive()) session.logout();
        }
        sessions.clear();

        // Shut down the engines ...
        if (engine1 != null) engine1.shutdown();
        if (engine2 != null) engine2.shutdown();
        if (engine3 != null) engine3.shutdown();
        engine1 = null;
        engine2 = null;
        engine3 = null;
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Tests
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldAllowMultipleEnginesToAccessSameDatabase() throws Exception {
        Session session1 = sessionFrom(engine1);
        Node node1 = session1.getRootNode().getNode("Cars/Hybrid/Toyota Highlander");
        assertThat(node1, is(notNullValue()));

        Session session2 = sessionFrom(engine2);
        Node node = session2.getRootNode().getNode("Cars/Hybrid/Toyota Highlander");
        assertThat(node, is(notNullValue()));
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility Methods
    // ----------------------------------------------------------------------------------------------------------------

    protected Session sessionFrom( JcrEngine engine ) throws RepositoryException {
        Repository repository = engine.getRepository("cars");
        Session session = repository.login();
        sessions.add(session);
        return session;
    }

    protected static URL resourceUrl( String name ) {
        return ClusteringTest.class.getClassLoader().getResource(name);
    }

    protected static InputStream resourceStream( String name ) {
        return ClusteringTest.class.getClassLoader().getResourceAsStream(name);
    }

}
