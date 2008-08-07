/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import org.jboss.dna.spi.DnaLexicon;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.ExecutionContextFactory;
import org.jboss.dna.spi.connector.BasicExecutionContext;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.connector.RepositoryConnectionFactory;
import org.jboss.dna.spi.connector.RepositorySourceException;
import org.jboss.dna.spi.connector.SimpleRepository;
import org.jboss.dna.spi.connector.SimpleRepositorySource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedRepositorySourceTest {

    private FederatedRepositorySource source;
    private String sourceName;
    private String repositoryName;
    private String username;
    private String credentials;
    private String executionContextFactoryJndiName;
    private String repositoryConnectionFactoryJndiName;
    private String configurationSourceName;
    private String securityDomain;
    private SimpleRepository configRepository;
    private SimpleRepositorySource configRepositorySource;
    private RepositoryConnection configRepositoryConnection;
    private ExecutionContext context;
    @Mock
    private RepositoryConnection connection;
    @Mock
    private Context jndiContext;
    @Mock
    private RepositoryConnectionFactory connectionFactory;
    @Mock
    private ExecutionContextFactory executionContextFactory;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new BasicExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.NAMESPACE_PREFIX, DnaLexicon.NAMESPACE_URI);
        executionContextFactoryJndiName = "context factory jndi name";
        repositoryConnectionFactoryJndiName = "repository connection factory jndi name";
        configurationSourceName = "configuration source name";
        repositoryName = "Test Repository";
        securityDomain = "security domain";
        source = new FederatedRepositorySource(repositoryName);
        sourceName = "federated source";
        username = "valid username";
        credentials = "valid password";
        source.setName(sourceName);
        source.setUsername(username);
        source.setPassword(credentials);
        source.setConfigurationSourceName(configurationSourceName);
        source.setConfigurationSourceProjectionRules(new String[] {"/dna:system/dna:federation/ => /dna:repositories/Test Repository"});
        source.setRepositoryConnectionFactoryJndiName(repositoryConnectionFactoryJndiName);
        source.setExecutionContextFactoryJndiName(executionContextFactoryJndiName);
        source.setContext(jndiContext);
        source.setSecurityDomain(securityDomain);
        configRepository = new SimpleRepository("Configuration Repository");
        configRepository.setProperty(context, "/dna:repositories/Test Repository", "dna:timeToExpire", "100000");
        configRepository.setProperty(context, "/dna:repositories/Test Repository", "dna:timeToCache", "100000");
        configRepository.setProperty(context, "/dna:repositories/Test Repository/dna:cache", "dna:projectionRules", "/ => /");
        configRepository.setProperty(context,
                                     "/dna:repositories/Test Repository/dna:projections/source 1/",
                                     "dna:projectionRules",
                                     "/ => /s1");
        configRepository.setProperty(context,
                                     "/dna:repositories/Test Repository/dna:projections/source 2/",
                                     "dna:projectionRules",
                                     "/ => /s1");
        configRepositorySource = new SimpleRepositorySource();
        configRepositorySource.setRepositoryName(configRepository.getRepositoryName());
        configRepositorySource.setName(configurationSourceName);
        configRepositoryConnection = configRepositorySource.getConnection();
        stub(connectionFactory.createConnection(configurationSourceName)).toReturn(configRepositoryConnection);
        stub(jndiContext.lookup(executionContextFactoryJndiName)).toReturn(executionContextFactory);
        stub(jndiContext.lookup(repositoryConnectionFactoryJndiName)).toReturn(connectionFactory);
        stub(executionContextFactory.create(eq(securityDomain), anyCallbackHandler())).toReturn(context);
    }

    protected static CallbackHandler anyCallbackHandler() {
        return argThat(new ArgumentMatcher<CallbackHandler>() {
            @Override
            public boolean matches( Object callback ) {
                return callback != null;
            }
        });
    }

    @After
    public void afterEach() throws Exception {
        SimpleRepository.shutdownAll();
    }

    @Test
    public void shouldReturnNonNullCapabilities() {
        assertThat(source.getCapabilities(), is(notNullValue()));
    }

    @Test
    public void shouldSupportSameNameSiblings() {
        assertThat(source.getCapabilities().supportsSameNameSiblings(), is(true));
    }

    @Test
    public void shouldSupportUpdates() {
        assertThat(source.getCapabilities().supportsUpdates(), is(true));
    }

    @Test
    public void shouldCreateConnectionsByAuthenticateUsingFederationRepository() throws Exception {
        connection = source.getConnection();
        assertThat(connection, is(notNullValue()));
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldNotCreateConnectionWhenAuthenticationFails() throws Exception {
        // Stub the execution context factory to throw a LoginException to simulate failed authentication
        stub(executionContextFactory.create(eq(securityDomain), anyCallbackHandler())).toThrow(new LoginException());
        source.getConnection();
    }

    @Test( expected = NullPointerException.class )
    public void shouldPropogateAllExceptionsExceptLoginExceptionThrownFromExecutionContextFactory() throws Exception {
        // Stub the execution context factory to throw a LoginException to simulate failed authentication
        stub(executionContextFactory.create(eq(securityDomain), anyCallbackHandler())).toThrow(new NullPointerException());
        source.getConnection();
    }

    @Test
    public void shouldHaveNameSuppliedInConstructor() {
        source = new FederatedRepositorySource(repositoryName);
        assertThat(source.getRepositoryName(), is(repositoryName));
    }

    @Test
    public void shouldHaveNullSourceNameUponConstruction() {
        source = new FederatedRepositorySource(repositoryName);
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingName() {
        source.setName("Something");
        assertThat(source.getName(), is("Something"));
        source.setName("another name");
        assertThat(source.getName(), is("another name"));
    }

    @Test
    public void shouldAllowSettingNameToNull() {
        source.setName("some name");
        source.setName(null);
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingUsername() {
        source.setUsername("Something");
        assertThat(source.getUsername(), is("Something"));
        source.setUsername("another name");
        assertThat(source.getUsername(), is("another name"));
    }

    @Test
    public void shouldAllowSettingUsernameToNull() {
        source.setUsername("some name");
        source.setUsername(null);
        assertThat(source.getUsername(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingCredentials() {
        source.setPassword("Something");
        assertThat(source.getPassword(), is("Something"));
        source.setPassword("another name");
        assertThat(source.getPassword(), is("another name"));
    }

    @Test
    public void shouldAllowSettingCredentialsToNull() {
        source.setPassword("some name");
        source.setPassword(null);
        assertThat(source.getPassword(), is(nullValue()));
    }

    @Test
    public void shouldHaveDefaultRetryLimit() {
        assertThat(source.getRetryLimit(), is(FederatedRepositorySource.DEFAULT_RETRY_LIMIT));
    }

    @Test
    public void shouldSetRetryLimitToZeroWhenSetWithNonPositiveValue() {
        source.setRetryLimit(0);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-1);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-100);
        assertThat(source.getRetryLimit(), is(0));
    }

    @Test
    public void shouldAllowRetryLimitToBeSet() {
        for (int i = 0; i != 100; ++i) {
            source.setRetryLimit(i);
            assertThat(source.getRetryLimit(), is(i));
        }
    }

    @Test
    public void shouldCreateJndiReferenceAndRecreatedObjectFromReference() throws Exception {
        int retryLimit = 100;
        source.setPassword(credentials);
        source.setUsername(username);
        source.setRetryLimit(retryLimit);
        source.setName("Some source");
        source.setConfigurationSourceName("config source");
        source.setConfigurationSourceProjectionRules(new String[] {"/dna:system => /a/b/c"});
        source.setRepositoryConnectionFactoryJndiName("repository connection factory jndi name");
        source.setRepositoryJndiName("repository jndi name");
        source.setExecutionContextFactoryJndiName("env jndi name");

        Reference ref = source.getReference();
        assertThat(ref.getClassName(), is(FederatedRepositorySource.class.getName()));
        assertThat(ref.getFactoryClassName(), is(FederatedRepositorySource.class.getName()));

        Map<String, Object> refAttributes = new HashMap<String, Object>();
        Enumeration<RefAddr> enumeration = ref.getAll();
        while (enumeration.hasMoreElements()) {
            RefAddr addr = enumeration.nextElement();
            refAttributes.put(addr.getType(), addr.getContent());
        }

        assertThat((String)refAttributes.remove(FederatedRepositorySource.USERNAME), is(username));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.PASSWORD), is(credentials));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.SOURCE_NAME), is(source.getName()));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.REPOSITORY_NAME), is(repositoryName));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.RETRY_LIMIT), is(Integer.toString(retryLimit)));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.CONFIGURATION_SOURCE_NAME),
                   is(source.getConfigurationSourceName()));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.CONFIGURATION_SOURCE_PROJECTION_RULES),
                   is("/dna:system => /a/b/c"));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.REPOSITORY_CONNECTION_FACTORY_JNDI_NAME),
                   is(source.getRepositoryConnectionFactoryJndiName()));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.EXECUTION_CONTEXT_FACTORY_JNDI_NAME),
                   is(source.getExecutionContextFactoryJndiName()));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.REPOSITORY_JNDI_NAME),
                   is(source.getRepositoryJndiName()));
        assertThat((String)refAttributes.remove(FederatedRepositorySource.SECURITY_DOMAIN), is(securityDomain));
        assertThat(refAttributes.isEmpty(), is(true));

        // Recreate the object, use a newly constructed source ...
        ObjectFactory factory = new FederatedRepositorySource();
        Name name = mock(Name.class);
        Context context = mock(Context.class);
        Hashtable<?, ?> env = new Hashtable<Object, Object>();
        FederatedRepositorySource recoveredSource = (FederatedRepositorySource)factory.getObjectInstance(ref, name, context, env);
        assertThat(recoveredSource, is(notNullValue()));

        assertThat(recoveredSource.getName(), is(source.getName()));
        assertThat(recoveredSource.getUsername(), is(source.getUsername()));
        assertThat(recoveredSource.getPassword(), is(source.getPassword()));
        assertThat(recoveredSource.getRepositoryName(), is(source.getRepositoryName()));
        assertThat(recoveredSource.getRetryLimit(), is(source.getRetryLimit()));
        assertThat(recoveredSource.getConfigurationSourceName(), is(source.getConfigurationSourceName()));
        assertThat(recoveredSource.getConfigurationSourceProjectionRules(), is(source.getConfigurationSourceProjectionRules()));
        assertThat(recoveredSource.getRepositoryConnectionFactoryJndiName(), is(source.getRepositoryConnectionFactoryJndiName()));
        assertThat(recoveredSource.getExecutionContextFactoryJndiName(), is(source.getExecutionContextFactoryJndiName()));
        assertThat(recoveredSource.getRepositoryJndiName(), is(source.getRepositoryJndiName()));
        assertThat(recoveredSource.getSecurityDomain(), is(source.getSecurityDomain()));

        assertThat(recoveredSource.equals(source), is(true));
        assertThat(source.equals(recoveredSource), is(true));
    }
}
