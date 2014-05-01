/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.modeshape.jca;

import static org.junit.Assert.assertNotNull;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.jca.embedded.Embedded;
import org.jboss.jca.embedded.EmbeddedFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.AbstractTransactionalTest;

/**
 * ConnectorTestCase
 *
 * @author kulikov
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ConnectorTestCase extends AbstractTransactionalTest {
    private static final String CONN_FACTORY_JNDI_NAME = "java:/eis/JcrCciConnectionFactory";

    private static Embedded embedded;

    @BeforeClass
    public static void beforeClass() throws Throwable {
        // Create and set an embedded JCA instance
        embedded = EmbeddedFactory.create();

        // Startup
        embedded.startup();
    }


    @AfterClass
    public static void afterClass() throws Throwable {
        try {
            // Shutdown embedded
            embedded.shutdown();
        } catch (Throwable throwable) {
            //ignore
        }

        // Set embedded to null
        embedded = null;
    }
    @Test
    public void testGetConnection() throws Throwable {
        Context context = null;

        String archiveName = "ConnectorTestCase_" + UUID.randomUUID().toString() + ".rar";
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, archiveName);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, UUID.randomUUID().toString() + ".jar");
        ja.addClasses(JcrResourceAdapter.class, JcrManagedConnectionFactory.class, JcrManagedConnection.class);
        ja.addAsResource("my-repository-config.json");
        raa.addAsLibrary(ja);
        raa.addAsManifestResource("ironjacamar.xml", "ironjacamar.xml");
        raa.addAsResource("my-repository-config.json");


        try {
            embedded.deploy(raa);

            context = new InitialContext();
            JcrRepositoryHandle connectionFactory = (JcrRepositoryHandle)context.lookup(CONN_FACTORY_JNDI_NAME);
            assertNotNull(connectionFactory);
            Session session = connectionFactory.getConnection();
            assertNotNull(session);
            Node node = session.getRootNode();
            assertNotNull(node);
            session.logout();
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException ne) {
                    // Ignore
                }
            }
            embedded.undeploy(raa);
        }
    }
}
