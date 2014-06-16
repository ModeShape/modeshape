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
package org.modeshape.jcr;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.modeshape.common.FixFor;
import org.modeshape.common.junit.SkipLongRunning;
import org.modeshape.common.junit.SkipTestRule;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.security.AdvancedAuthorizationProvider;
import org.modeshape.jcr.security.AuthenticationProvider;
import org.modeshape.jcr.security.AuthorizationProvider;
import org.modeshape.jcr.security.JaasSecurityContext.UserPasswordCallbackHandler;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.StringFactory;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AuthenticationAndAuthorizationTest {

    @Rule
    public TestRule skipTestRule = new SkipTestRule();

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

    private Environment environment;
    protected JcrRepository repository;
    protected JcrSession session;

    @Before
    public void beforeEach() throws Exception {
        environment = new TestingEnvironment();
    }

    @After
    public void afterEach() throws Exception {
        if (repository != null) {
            try {
                TestingUtil.killRepositories(repository);
            } finally {
                repository = null;
                session = null;
                environment.shutdown();
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
        RepositoryConfiguration config = new RepositoryConfiguration(doc, repoName, environment);
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
     *                 "classname" : "JAAS",
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
     *                 "classname" : "JAAS",
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
            EditableDocument jaas = Schematic.newDocument(FieldName.CLASSNAME, "JAAS", "policyName", "modeshape-jcr");
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

    @SuppressWarnings( "cast" )
    @Test
    public void shouldAllowLoginWithNoCredentialsInPrivilegedBlock() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr";
        String[] anonRoleNames = {ModeShapeRoles.READWRITE};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);
        startRepositoryWith(config, repoName);

        // Verify the JAAS was configured correctly ...
        session = repository.login(new SimpleCredentials("readwrite", "readwrite".toCharArray()));

        LoginContext login = new LoginContext("modeshape-jcr", new UserPasswordCallbackHandler("superuser",
                                                                                               "superuser".toCharArray()));
        login.login();

        Subject subject = login.getSubject();

        Session session = (Session)Subject.doAsPrivileged(subject, new PrivilegedExceptionAction<Session>() {

            @Override
            public Session run() throws Exception {
                return repository.login();
            }

        }, AccessController.getContext());

        assertThat(session, is(notNullValue()));
        assertThat(session.getUserID(), is("superuser"));
        login.logout();
    }

    @Test( expected = javax.jcr.LoginException.class )
    public void shouldNotAllowLoginIfCredentialsDoNotProvideJaasMethod() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr";
        String[] anonRoleNames = {};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        repository.login(new Credentials() {
            private static final long serialVersionUID = 1L;
        });
    }

    @Test( expected = javax.jcr.LoginException.class )
    public void shouldNotAllowLoginIfCredentialsReturnNullAccessControlContext() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr";
        String[] anonRoleNames = {};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        repository.login(new Credentials() {
            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public AccessControlContext getAccessControlContext() {
                return null;
            }
        });
    }

    @Test( expected = javax.jcr.LoginException.class )
    public void shouldNotAllowLoginIfCredentialsReturnNullLoginContext() throws Exception {
        String repoName = REPO_NAME;
        String jaasPolicyName = "modeshape-jcr";
        String[] anonRoleNames = {};
        Document config = createRepositoryConfiguration(repoName, jaasPolicyName, anonRoleNames);

        startRepositoryWith(config, repoName);

        repository.login(new Credentials() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public LoginContext getLoginContext() {
                return null;
            }
        });
    }

    @Test
    @FixFor( "MODE-1938" )
    @SkipLongRunning
    public void shouldNotLeakUninitializedWorkspaceCaches() throws Exception {
        int runCount = 200;
        for (int i = 0; i < runCount; i++) {
            beforeEach();
            shouldLogInAsAnonymousUserIfNoProviderAuthenticatesCredentials();
            afterEach();
        }
    }

    @Test
    @FixFor( "MODE-2225" )
    public void shouldInvokeAuthorizationProviderWhenIteratingNodes() throws Exception {
        //this will import an initial structure of nodes (see xmlImport/docWithChildren.xml)
        repository = TestingUtil.startRepositoryWithConfig("config/custom-authentication-provider-config-1.json");
        assertPermissionsCheckedWhenIteratingChildren();
    }

    @Test
    @FixFor( "MODE-2225" )
    public void shouldInvokeAdvancedAuthorizationProviderWhenIteratingNodes() throws Exception {
        //this will import an initial structure of nodes (see xmlImport/docWithChildren.xml)
        repository = TestingUtil.startRepositoryWithConfig("config/custom-authentication-provider-config-2.json");
        assertPermissionsCheckedWhenIteratingChildren();
    }

    private void assertPermissionsCheckedWhenIteratingChildren() throws RepositoryException {
        session = repository.login();
        Node testRoot = session.getNode("/testRoot");
        NodeIterator children = testRoot.getNodes();
        while (children.hasNext()) {
            children.nextNode();
        }
        TestSecurityProvider provider = (TestSecurityProvider) session.context().getSecurityContext();

        Map<String, String> actionsByNodePath = provider.getActionsByNodePath();
        assertTrue("READ permission not checked for node", actionsByNodePath.containsKey("/testRoot"));
        assertTrue("READ permission not checked for node", actionsByNodePath.containsKey("/testRoot/node3"));
        assertTrue("READ permission not checked for node", actionsByNodePath.containsKey("/testRoot/node2"));
        assertTrue("READ permission not checked for node", actionsByNodePath.containsKey("/testRoot/node1"));
    }

    public static abstract class TestSecurityProvider implements AuthenticationProvider, SecurityContext {
        protected final Map<String, String> actionsByNodePath = new HashMap<>();
        protected StringFactory stringFactory;

        @Override
        public ExecutionContext authenticate( Credentials credentials, String repositoryName, String workspaceName,
                                              ExecutionContext repositoryContext, Map<String, Object> sessionAttributes ) {
            stringFactory = repositoryContext.getValueFactories().getStringFactory();
            return repositoryContext.with(this);
        }

        @Override
        public boolean isAnonymous() {
            return false;
        }

        @Override
        public String getUserName() {
            return "test user";
        }

        @Override
        public boolean hasRole( String roleName ) {
            return true;
        }

        @Override
        public void logout() {
        }

        public Map<String, String> getActionsByNodePath() {
            return actionsByNodePath;
        }
    }

    public static class SimpleTestSecurityProvider extends TestSecurityProvider implements AuthorizationProvider {
        @Override
        public boolean hasPermission( ExecutionContext context, String repositoryName, String repositorySourceName,
                                      String workspaceName, Path absPath, String... actions ) {
            actionsByNodePath.put(stringFactory.create(absPath), actions[0]);
            return true;
        }

    }

    public static class AdvancedTestSecurityProvider extends TestSecurityProvider implements AdvancedAuthorizationProvider {
        @Override
        public boolean hasPermission( Context context, Path absPath, String... actions ) {
            actionsByNodePath.put(stringFactory.create(absPath), actions[0]);
            return true;
        }
    }
}
