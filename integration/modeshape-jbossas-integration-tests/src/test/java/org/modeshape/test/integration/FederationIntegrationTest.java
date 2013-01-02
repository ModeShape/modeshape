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
import javax.annotation.Resource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.federation.FederationManager;
import static junit.framework.Assert.assertNotNull;

/**
 * Integration test which verifies that various external sources are correctly set-up via the JBoss AS subsystem.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class )
public class FederationIntegrationTest {

    @Resource( mappedName = "/jcr/federatedRepository" )
    private JcrRepository repository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "federatedRepository-test.war");
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Test
    public void shouldLinkFileSystemSource() throws Exception {
        Session defaultSession = repository.login();
        //predefined
        assertNotNull(defaultSession.getNode("/projection1"));

        FederationManager federationManager = defaultSession.getWorkspace().getFederationManager();
        federationManager.createProjection("/", "filesystem", "/", "testProjection");
        assertNotNull(defaultSession.getNode("/testProjection"));

        Session otherSession = repository.login("other");
        //predefined
        assertNotNull(otherSession.getNode("/projection1"));
    }
}
