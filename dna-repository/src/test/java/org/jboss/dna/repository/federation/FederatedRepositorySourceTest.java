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
package org.jboss.dna.repository.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedRepositorySourceTest {

    private FederatedRepositorySource source;
    @Mock
    private FederatedRepositoryConnection connection;
    @Mock
    private FederatedRepository repository;
    @Mock
    private FederationService service;
    private String repositoryName;
    private String username;
    private String credentials;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.repositoryName = "Test Repository";
        this.source = new FederatedRepositorySource(service, repositoryName);
        this.username = "valid username";
        this.credentials = "valid password";
        this.source.setUsername(username);
        this.source.setCredentials(credentials);
    }

    @Test
    public void shouldCreateConnectionsByAuthenticateUsingFederationRepository() throws Exception {
        stub(repository.createConnection(source, source.getUsername(), source.getCredentials())).toReturn(connection);
        stub(service.getRepository(source.getRepositoryName())).toReturn(repository);
        connection = (FederatedRepositoryConnection)source.getConnection();
        assertThat(connection, is(sameInstance(connection)));
        verify(repository, times(1)).createConnection(source, source.getUsername(), source.getCredentials());
        verify(service, times(1)).getRepository(source.getRepositoryName());
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldNotCreateConnectionWhenAuthenticationFails() throws Exception {
        stub(repository.createConnection(source, source.getUsername(), source.getCredentials())).toReturn(null);
        stub(service.getRepository(source.getRepositoryName())).toReturn(repository);
        connection = (FederatedRepositoryConnection)source.getConnection();
    }

    @Test
    public void shouldHaveNameSuppliedInConstructor() {
        assertThat(source.getRepositoryName(), is(repositoryName));
    }

    @Test
    public void shouldHaveNullSourceNameUponConstruction() {
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
        source.setCredentials("Something");
        assertThat(source.getCredentials(), is("Something"));
        source.setCredentials("another name");
        assertThat(source.getCredentials(), is("another name"));
    }

    @Test
    public void shouldAllowSettingCredentialsToNull() {
        source.setCredentials("some name");
        source.setCredentials(null);
        assertThat(source.getCredentials(), is(nullValue()));
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
        String serviceJndiName = "jndiName";
        stub(service.getJndiName()).toReturn(serviceJndiName);

        int retryLimit = 100;
        source.setCredentials(credentials);
        source.setUsername(username);
        source.setRetryLimit(retryLimit);
        source.setName("Some source");

        Reference ref = source.getReference();
        assertThat(ref.getClassName(), is(FederatedRepositorySource.class.getName()));
        assertThat(ref.getFactoryClassName(), is(FederatedRepositorySource.NamingContextObjectFactory.class.getName()));

        Map<String, Object> refAttributes = new HashMap<String, Object>();
        Enumeration<RefAddr> enumeration = ref.getAll();
        while (enumeration.hasMoreElements()) {
            RefAddr addr = enumeration.nextElement();
            refAttributes.put(addr.getType(), addr.getContent());
        }

        assertThat(refAttributes.remove(FederatedRepositorySource.USERNAME), is((Object)username));
        assertThat(refAttributes.remove(FederatedRepositorySource.CREDENTIALS), is((Object)credentials));
        assertThat(refAttributes.remove(FederatedRepositorySource.SOURCE_NAME), is((Object)source.getName()));
        assertThat(refAttributes.remove(FederatedRepositorySource.REPOSITORY_NAME), is((Object)repositoryName));
        assertThat(refAttributes.remove(FederatedRepositorySource.FEDERATION_SERVICE_JNDI_NAME), is((Object)serviceJndiName));
        assertThat(refAttributes.remove(FederatedRepositorySource.RETRY_LIMIT), is((Object)Integer.toString(retryLimit)));
        assertThat(refAttributes.isEmpty(), is(true));

        // Recreate the object, use a newly constructed source ...
        FederatedRepositorySource.NamingContextObjectFactory factory = new FederatedRepositorySource.NamingContextObjectFactory();
        Name name = mock(Name.class);
        Context context = mock(Context.class);
        stub(context.lookup(serviceJndiName)).toReturn(service);
        Hashtable<?, ?> env = new Hashtable<Object, Object>();
        FederatedRepositorySource recoveredSource = (FederatedRepositorySource)factory.getObjectInstance(ref, name, context, env);
        assertThat(recoveredSource, is(notNullValue()));

        assertThat(recoveredSource.getName(), is(source.getName()));
        assertThat(recoveredSource.getUsername(), is(source.getUsername()));
        assertThat(recoveredSource.getCredentials(), is(source.getCredentials()));
        assertThat(recoveredSource.getRepositoryName(), is(source.getRepositoryName()));
        assertThat(recoveredSource.getRetryLimit(), is(source.getRetryLimit()));
        assertThat(recoveredSource.getFederationService(), is(service));

        assertThat(recoveredSource.equals(source), is(true));
        assertThat(source.equals(recoveredSource), is(true));
    }
}
