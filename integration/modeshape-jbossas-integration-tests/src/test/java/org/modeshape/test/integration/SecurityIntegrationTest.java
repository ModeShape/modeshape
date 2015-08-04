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
package org.modeshape.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Resource;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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

    @Resource( mappedName = "java:/jcr/defaultAnonymousRepository" )
    private JcrRepository defaultAnonymousRepository;

    @Resource( mappedName = "java:/jcr/customAuthenticatorRepository" )
    private JcrRepository customAuthenticatorRepository;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "security-test.war")
                                       .addClass(CustomAuthenticationProvider.class)
                                       .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Before
    public void before() {
        assertNotNull(sampleRepo);
        assertNotNull(anonymousRepository);
        assertNotNull(defaultAnonymousRepository);
    }

    @Test
    @FixFor( "MODE-2411" )
    public void shouldAuthenticateUsingJBossProvider() throws Exception {
        JcrSession adminSession = sampleRepo.login(new SimpleCredentials("admin", "admin".toCharArray()));
        assertNotNull(adminSession);
        assertTrue(adminSession.hasPermission("/", permissionsString(ModeShapePermissions.ALL_PERMISSIONS)));
        adminSession.logout();

        JcrSession guestSession = sampleRepo.login(new SimpleCredentials("guest", "guest".toCharArray()));
        assertNotNull(guestSession);
        assertTrue(guestSession.hasPermission("/", ModeShapePermissions.READ));
        assertFalse(guestSession.hasPermission("/", permissionsString(ModeShapePermissions.ALL_CHANGE_PERMISSIONS)));
        guestSession.logout();

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

    @Test
    @FixFor( "MODE-2228" )
    public void readonlyShouldBeTheDefaultAnonymousRole() throws Exception {
        Session session = defaultAnonymousRepository.login();
        session.getRootNode();
        try {
            session.getRootNode().addNode("test");
            fail("The default anonymous role should be 'readonly'");
        } catch (AccessDeniedException e) {
           //expected
        }
    }

    @Test
    @FixFor( "MODE-2496" )
    public void customAuthenticationProvidersShouldBeInvokedFirst() throws Exception {
        // authenticate using some credentials which are part of the default security domain and check that the custom
        // provider is invoked first and roles are granted which are not configured as such in the security domain
        Session session = customAuthenticatorRepository.login(new SimpleCredentials("guest", "guest".toCharArray()));
        try {
            assertEquals("arquillian", session.getUserID());
            session.checkPermission("/", "admin");
        } finally {
            session.logout();
        }
    }
}
