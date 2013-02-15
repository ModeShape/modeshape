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

import java.io.File;
import javax.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;

/**
 * Integration test around retrieving a repository from the AS7 container using CDI.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class)
public class CDITest {

    @Deployment(name = "cdi-test-war")
    public static WebArchive createWarDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "cdi-war-test.war")
                                       .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                                       .addClass(CDIRepositoryProvider.class)
                                       .addClass(CDIRepositoryConsumer.class);


        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    /**
     * Creates the following deployable:
     *
     * cdi-test-ear.ear
     *     /lib/cdi-ear-producer.jar
     *              /META-INF/beans.xml
     *     /cdi-ear-test.war
     *             /WEB-INF/beans.xml
     *     /META-INF/MANIFEST.MF
     */
    @Deployment(name = "cdi-test-ear")
    public static EnterpriseArchive createEarDeployment() {

        JavaArchive providerLib = ShrinkWrap.create(JavaArchive.class, "cdi-ear-producer.jar")
                                                     .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create(
                                                             "beans.xml"))
                                                     .addClass(CDIRepositoryProvider.class);

        WebArchive webapp = ShrinkWrap.create(WebArchive.class, "cdi-ear-test.war")
                                      .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                                      .addClass(CDITest.class)
                                      .addClass(CDIRepositoryConsumer.class);

        return ShrinkWrap.create(EnterpriseArchive.class, "cdi-ear-test.ear")
                                          .addAsModule(webapp)
                                          .addAsLibraries(providerLib)
                                          .addAsManifestResource(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }

    @Inject
    private CDIRepositoryConsumer consumer;

    @Test
    @OperateOnDeployment("cdi-test-war")
    public void testInjectionInWar() throws Exception {
        assertNotNull(consumer.nodeAt("/"));
        assertNotNull(consumer.importContentHandler("/"));
    }

    @Test
    @OperateOnDeployment("cdi-test-ear")
    public void testInjectionInEar() throws Exception {
        assertNotNull(consumer.nodeAt("/"));
        assertNotNull(consumer.importContentHandler("/"));
    }
}
