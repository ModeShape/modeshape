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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.concurrent.TimeUnit;
import javax.jcr.LoginException;
import javax.jcr.SimpleCredentials;
import org.infinispan.config.Configuration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AuthenticationAndAuthorizationTest {

    private static final String REPO_NAME = "testRepo";

    @BeforeClass
    public static void beforeAll() {
        // Initialize PicketBox ...
        JaasTestUtil.initJaas("security/jaas.conf.xml");
    }

    @AfterClass
    public static void afterAll() {
        JaasTestUtil.releaseJaas();
    }

    private CacheContainer cm;
    protected JcrRepository repository;
    protected JcrSession session;

    @Before
    public void beforeEach() throws Exception {
        Configuration c = new Configuration();
        c = c.fluent().transaction().transactionManagerLookup(new DummyTransactionManagerLookup()).build();
        cm = TestCacheManagerFactory.createCacheManager(c);
    }

    @After
    public void afterEach() throws Exception {
        if (repository != null) {
            try {
                repository.shutdown().get(3L, TimeUnit.SECONDS);
            } finally {
                repository = null;
                session = null;
                try {
                    TestingUtil.killCacheManagers(cm);
                } finally {
                    cm = null;
                }
            }
        }
    }

    /**
     * Start the repository using the supplied repository configuration. Note that this does <i>not</i> create a session.
     * 
     * @param doc the document containing the configuration; may not be null
     * @param repoName the name of the repository; may not be null
     * @throws Exception if there is a problem starting the repository
     */
    protected void startRepositoryWith( Document doc,
                                        String repoName ) throws Exception {
        RepositoryConfiguration config = new RepositoryConfiguration(doc, repoName, cm);
        repository = new JcrRepository(config);
        repository.start();
    }

    /**
     * Creates a repository configuration that uses the specified JAAS provider and optionally enables anonymous logins.
     * <p>
     * The configuration for "repositoryName" as the repository name, anonymous logins disabled, and "modeshape-jcr" as the JAAS
     * policy looks as follows:
     * 
     * <pre>
     * {
     *     "name" : "repositoryNameParameter";
     *     "security" : {
     *         "anonymous" : {
     *             "roles" : []
     *         },
     *         "providers" : [
     *             {
     *                 "type" : "JAAS",
     *                 "policyName" : "modeshape-jcr"
     *             }
     *         ]
     *     }
     * }
     * </pre>
     * 
     * If anonymous logins <i>are</i> enabled, then they are also enabled on failed logins:
     * 
     * <pre>
     * {
     *     "name" : "repositoryNameParameter";
     *     "security" : {
     *         "anonymous" : {
     *             "useOnFailedLogin" : true
     *         },
     *         "providers" : [
     *             {
     *                 "type" : "JAAS",
     *                 "policyName" : "modeshape-jcr"
     *             }
     *         ]
     *     }
     * }
     * </pre>
     * 
     * </p>
     * 
     * @param repositoryName the name of the repository; may not be null
     * @param jaasPolicyName the name of the jaas policy; may be null if JAAS should be not be enabled
     * @param anonymousRoleNames the anonymous role names, or empty if anonymous logins should be disabled
     * @return the configuration document; never null
     */
    protected Document createRepositoryConfiguration( String repositoryName,
                                                      String jaasPolicyName,
                                                      String... anonymousRoleNames ) {
        EditableDocument doc = Schematic.newDocument("name", repositoryName);
        EditableDocument security = doc.getOrCreateDocument("security");

        if (anonymousRoleNames == null || anonymousRoleNames.length == 0) {
            // Disable anonymous logins ...
            EditableDocument anonymous = security.getOrCreateDocument("anonymous");
            anonymous.setArray("roles");
        } else {
            // Set the roles and use on failed logins ...
            EditableDocument anonymous = security.getOrCreateDocument("anonymous");
            anonymous.setArray("roles", (Object[])anonymousRoleNames);
            anonymous.setBoolean("useOnFailedLogin", true);
        }

        if (jaasPolicyName != null) {
            // Add the JAAS provider ...
            EditableArray providers = security.getOrCreateArray("providers");
            EditableDocument jaas = Schematic.newDocument("type", "JAAS", "policyName", "modeshape-jcr");
            providers.addDocument(jaas);
        }

        return doc;
    }

    @Test
    public void shouldLogInAsAnonymousUsingNoCredentials() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr-non-existant";
        String[] anonRoleNames = {ModeShapeRoles.READWRITE};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        session = repository.login();
        session.getRootNode().getPath();
        session.getRootNode().addNode("someNewNode");
    }

    @Test
    public void shouldLogInAsAnonymousWithReadOnlyPrivilegesUsingNoCredentials() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr-non-existant";
        String[] anonRoleNames = {ModeShapeRoles.READONLY};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        session = repository.login();
        session.getRootNode().getPath();
        try {
            session.getRootNode().addNode("someNewNode");
            fail("Should not have been able to update content with a read-only user");
        } catch (javax.jcr.AccessDeniedException e) {
            // expected
        }
    }

    @Test
    public void shouldLogInAsUserWithReadOnlyRole() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr";
        String[] anonRoleNames = {};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        session = repository.login(new SimpleCredentials(ModeShapeRoles.READONLY, ModeShapeRoles.READONLY.toCharArray()));

        session.getRootNode().getPath();
        session.getRootNode().getDefinition();
        try {
            session.getRootNode().addNode("someNewNode");
            fail("Should not have been able to update content with a read-only user");
        } catch (javax.jcr.AccessDeniedException e) {
            // expected
        }
    }

    @Test
    public void shouldLogInAsUserWithReadWriteRole() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr";
        String[] anonRoleNames = {};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        session = repository.login(new SimpleCredentials("readwrite", "readwrite".toCharArray()));

        session.getRootNode().getPath();
        session.getRootNode().getDefinition();
        session.getRootNode().addNode("someNewNode");
    }

    @Test
    public void shouldNotAllowAnonymousLoginsWhenUsingOnlyJaas() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr";
        String[] anonRoleNames = {};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        try {
            session = repository.login();
            fail("Should not have been able to login anonymously if anonymous logins are disabled");
        } catch (LoginException e) {
            // expected
        }
    }

    @Test
    public void shouldLogInAsAnonymousUserIfNoProviderAuthenticatesCredentials() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr";
        String[] anonRoleNames = {ModeShapeRoles.READONLY};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        session = repository.login(new SimpleCredentials("readwrite", "wrongpassword".toCharArray()));

        assertThat(session.isAnonymous(), is(true));

        session.getRootNode().getPath();
        session.getRootNode().getDefinition();
        try {
            session.getRootNode().addNode("someNewNode");
            fail("Should not have been able to update content with a read-only user");
        } catch (javax.jcr.AccessDeniedException e) {
            // expected
        }
    }

    @Test
    public void shouldLogInAsWritableAnonymousUserIfNoProviderAuthenticatesCredentials() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr";
        String[] anonRoleNames = {ModeShapeRoles.READWRITE};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        session = repository.login(new SimpleCredentials("readwrite", "wrongpassword".toCharArray()));

        assertThat(session.isAnonymous(), is(true));

        session.getRootNode().getPath();
        session.getRootNode().getDefinition();
        session.getRootNode().addNode("someNewNode");
    }

}
