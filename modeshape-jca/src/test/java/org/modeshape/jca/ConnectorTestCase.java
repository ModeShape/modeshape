/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

/**
 * ConnectorTestCase
 *
 * @author kulikov
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ConnectorTestCase {
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