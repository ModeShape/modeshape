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
import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.Session;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.AbstractTransactionalTest;

/**
 * ConnectorTestCase
 * 
 * @author kulikov
 */
@RunWith( Arquillian.class )
public class ConnectorTestCase extends AbstractTransactionalTest {

    private static String deploymentName = "ConnectorTestCase";

    /**
     * Define the deployment
     * 
     * @return The deployment archive
     */
    @Deployment
    public static ResourceAdapterArchive createDeployment() {
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName + ".rar");
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, UUID.randomUUID().toString() + ".jar");
        ja.addClasses(JcrResourceAdapter.class, JcrManagedConnectionFactory.class, JcrManagedConnection.class);
        ja.addAsResource("my-repository-config.json");
        raa.addAsLibrary(ja);
        raa.addAsManifestResource("ironjacamar.xml", "ironjacamar.xml");
        raa.addAsResource("my-repository-config.json");
        return raa;
    }

    /**
     * Resource
     */
    @Resource( mappedName = "java:/eis/JcrCciConnectionFactory" )
    private javax.jcr.Repository repository;

    /**
     * Test getConnection
     * 
     * @exception Throwable Thrown if case of an error
     */
    @Test
    public void testGetConnection() throws Throwable {
        assertNotNull(repository);

        Session session = repository.login();
        assertNotNull(session);

        Node node = session.getRootNode();
        assertNotNull(node);

        session.logout();
    }
}
