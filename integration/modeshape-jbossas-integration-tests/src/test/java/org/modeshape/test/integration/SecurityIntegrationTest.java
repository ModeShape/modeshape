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
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Resource;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrSession;
import org.modeshape.jcr.ModeShapePermissions;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration test around various authentication and authorization providers.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class )
public class SecurityIntegrationTest {

    @Resource( mappedName = "java:/jcr/sample" )
    private JcrRepository sampleRepo;

    @Resource( mappedName = "java:/jcr/anonymousRepository" )
    private JcrRepository anonymousRepository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "security-test.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Before
    public void before() {
        assertNotNull(sampleRepo);
        assertNotNull(anonymousRepository);
    }

    @Test
    public void shouldAuthenticateUsingJAASProvider() throws Exception {
        JcrSession adminSession = sampleRepo.login(new SimpleCredentials("admin", "admin".toCharArray()));
        assertNotNull(adminSession);
        assertTrue(adminSession.hasPermission("/", permissionsString(ModeShapePermissions.ALL_PERMISSIONS)));

        JcrSession guestSession = sampleRepo.login(new SimpleCredentials("guest", "guest".toCharArray()));
        assertNotNull(guestSession);
        assertTrue(guestSession.hasPermission("/", ModeShapePermissions.READ));
        assertFalse(guestSession.hasPermission("/", permissionsString(ModeShapePermissions.ALL_CHANGE_PERMISSIONS)));

        try {
            sampleRepo.login(new SimpleCredentials("admin", "invalid".toCharArray()));
            fail("JAAS provider should not allow login with invalid credentials");
        } catch (RepositoryException e) {
            //expected
        }

        try {
            sampleRepo.login(new SimpleCredentials("guest", "invalid".toCharArray()));
            fail("JAAS provider should not allow login with invalid credentials");
        } catch (RepositoryException e) {
            //expected
        }

        try {
            sampleRepo.login(new SimpleCredentials("invalid", "invalid".toCharArray()));
            fail("JAAS provider should not allow login with invalid credentials");
        } catch (RepositoryException e) {
            //expected
        }
    }

    private String permissionsString(String...permissions) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<String> it = Arrays.asList(permissions).iterator(); it.hasNext();) {
            builder.append(it.next());
            if (it.hasNext()) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    @Test
    public void shouldAuthenticateUsingAnonymousProvider() throws Exception {
        JcrSession anonymousSession = sampleRepo.login();
        assertNotNull(anonymousSession);
        assertTrue(anonymousSession.isAnonymous());
        //readonly,readwrite,admin roles are configured as default by the subsystem
        assertTrue(anonymousSession.hasPermission("/", permissionsString(ModeShapePermissions.ALL_PERMISSIONS)));
    }

    @Test
    @FixFor( "MODE-2228" )
    public void shouldAllowDisablingOfAnonymousRoles() throws Exception {
        try {
            anonymousRepository.login();
            fail("Should not allow anonymous logins");
        } catch (javax.jcr.LoginException e) {
            //expected
        }
    }
}
