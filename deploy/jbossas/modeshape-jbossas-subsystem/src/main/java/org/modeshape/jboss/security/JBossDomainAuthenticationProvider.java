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
package org.modeshape.jboss.security;

import java.security.AccessController;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import org.jboss.security.AuthenticationManager;
import org.modeshape.jboss.service.RepositoryService;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.JaasCredentials;
import org.modeshape.jcr.security.EnvironmentAuthenticationProvider;
import org.modeshape.jcr.security.JaasSecurityContext;
import org.modeshape.jcr.security.JaccSubjectResolver;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.security.SimplePrincipal;

/**
 * {@link org.modeshape.jcr.security.EnvironmentAuthenticationProvider} used to interact with the security subsystem from a
 * JBoss Application Server.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @see <a href="http://issues.jboss.org/browse/MODE-2411">MODE-2411</a>
 */
public class JBossDomainAuthenticationProvider extends EnvironmentAuthenticationProvider {

    private static final org.jboss.logging.Logger LOGGER = org.jboss.logging.Logger.getLogger(
            JBossDomainAuthenticationProvider.class.getPackage().getName());
    
    private AuthenticationManager authenticationManager;
    private JaccSubjectResolver jaccSubjectResolver;
    
    @Override
    public void initialize() {
        String domainName = securityDomain();
        this.authenticationManager = environment().getSecurityManagementServiceInjector().getValue().getAuthenticationManager(domainName);
        assert this.authenticationManager != null;
        // any JBoss container should be JACC compliant, so the necessary jars should be present in the classpath
        this.jaccSubjectResolver = new JaccSubjectResolver();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Initialized JBoss authentication provider using the container's authentication manager....");
        }
    }

    @Override
    public ExecutionContext authenticate( Credentials credentials, String repositoryName, String workspaceName,
                                          ExecutionContext repositoryContext, Map<String, Object> sessionAttributes ) {
        if (credentials == null) {
            return getPreauthenticatedSubject(repositoryContext);
        }
        if (credentials instanceof SimpleCredentials) {
            return validateSimpleCredentials((SimpleCredentials)credentials, repositoryContext);
        }
        if (credentials instanceof JaasCredentials) {
            return getSubjectFromJaas((JaasCredentials)credentials, repositoryContext);             
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Unknown {0} implementation: {1}. Please user either {2} or {3}", Credentials.class.getName(),
                         credentials.getClass().getName(), SimpleCredentials.class.getName(), JaasCredentials.class.getName());
        }
        return null;
    }

    private ExecutionContext getSubjectFromJaas( JaasCredentials credentials, ExecutionContext repositoryContext ) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Looking for an active subject in the JaasCredentials instance...");
        }
        Subject subject = credentials.getLoginContext().getSubject();
        if (subject == null) {
            LOGGER.warn("Cannot authenticate because the JassCredentials instance has a login context with a null subject...");
            return null; 
        }
        return repositoryContext.with(new JBossSecurityContext(new JaasSecurityContext(subject)));
    }

    private ExecutionContext validateSimpleCredentials( SimpleCredentials credentials, ExecutionContext repositoryContext) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debugv("Authenticating {0} in the {1} security domain using the JBoss Server security manager", credentials.getUserID(),
                          securityDomain());
        }
        Subject subject = new Subject();
        if (authenticationManager.isValid(SimplePrincipal.newInstance(credentials.getUserID()), credentials.getPassword(),
                                          subject)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Authentication successful....");
            }
            return repositoryContext.with(new JBossSecurityContext(new JaasSecurityContext(subject)));
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debugv("Credentials for {0} are not valid for the {1} security domain", credentials.getUserID(), securityDomain());
            }
            return null;
        }
    }

    private ExecutionContext getPreauthenticatedSubject( ExecutionContext repositoryContext ) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received null credentials, attempting to search for an active subject on the calling thread via JACC");
        }
        // There are no credentials, so see if there is an authenticated Subject ...
        Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject != null) {
            // There is, so use this subject ...
            return repositoryContext.with(new JBossSecurityContext(new JaasSecurityContext(subject)));
        }
        subject = jaccSubjectResolver.resolveSubject();
        // there are no credentials and we failed to find a pre-authenticated subject, so return null.
        return subject != null ? repositoryContext.with(new JBossSecurityContext(new JaasSecurityContext(subject))) : null;
    }
     
    @Override
    protected RepositoryService environment() {
        return (RepositoryService)super.environment();
    }

    private final class JBossSecurityContext implements SecurityContext {
        private final JaasSecurityContext jaasSecurityContext;

        private JBossSecurityContext( JaasSecurityContext jaasSecurityContext ) {
            assert jaasSecurityContext != null;
            this.jaasSecurityContext = jaasSecurityContext;
        }

        @Override
        public boolean isAnonymous() {
            return jaasSecurityContext.isAnonymous();
        }

        @Override
        public String getUserName() {
            return jaasSecurityContext.getUserName();
        }

        @Override
        public boolean hasRole( String roleName ) {
            return jaasSecurityContext.hasRole(roleName);
        }

        @Override
        public void logout() {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Logging out security context....");
            }
            authenticationManager.logout(SimplePrincipal.newInstance(jaasSecurityContext.getUserName()), null);
            jaasSecurityContext.logout();
        }
    }
}
