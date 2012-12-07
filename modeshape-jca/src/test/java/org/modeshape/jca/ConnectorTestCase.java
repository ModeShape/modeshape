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

import java.util.UUID;
import java.util.logging.Logger;

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
import static org.junit.Assert.*;

import org.modeshape.jca.*;

/**
 * ConnectorTestCase
 *
 * @author kulikov
 */
@RunWith(Arquillian.class)
public class ConnectorTestCase {

    private static Logger log = Logger.getLogger("ConnectorTestCase");
    private static String deploymentName = "ConnectorTestCase";

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static ResourceAdapterArchive createDeployment() {
        ResourceAdapterArchive raa =
                ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName + ".rar");
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, UUID.randomUUID().toString() + ".jar");
        ja.addClasses(JcrResourceAdapter.class, JcrManagedConnectionFactory.class, JcrManagedConnection.class);
        ja.addAsResource("my-repository-config.json");
        raa.addAsLibrary(ja);

        raa.addAsManifestResource("META-INF/ironjacamar.xml", "ironjacamar.xml");
        raa.addAsResource("my-repository-config.json");
        return raa;
    }
    /**
     * Resource
     */
    @Resource(mappedName = "java:/eis/JcrCciConnectionFactory")
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
