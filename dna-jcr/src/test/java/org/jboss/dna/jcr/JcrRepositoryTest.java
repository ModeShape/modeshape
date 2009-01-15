/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.jcr;

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.ExecutionContextFactory;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrRepositoryTest {

    private Repository repository;
    @Mock
    private Map<String, String> descriptors;
    @Mock
    private ExecutionContextFactory executionContextFactory;
    @Mock
    private RepositoryConnectionFactory connectionFactory;
    @Mock
    private ExecutionContext executionContext;
    @Mock
    LoginContext loginContext;
    @Mock
    private RepositoryConnection connection;
    AccessControlContext accessControlContext = AccessController.getContext();
    private Credentials credentials = new Credentials() {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings( "unused" )
        public AccessControlContext getAccessControlContext() {
            return accessControlContext;
        }
    };

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        stub(executionContextFactory.create()).toReturn(executionContext);
        stub(executionContextFactory.create(accessControlContext)).toReturn(executionContext);
        stub(connectionFactory.createConnection(JcrI18n.defaultWorkspaceName.text())).toReturn(connection);
        repository = new JcrRepository(descriptors, executionContextFactory, connectionFactory);
    }

    @Test
    public void shouldAllowNoDescriptors() {
        new JcrRepository(descriptors, executionContextFactory, connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNoExecutionContextFactory() throws Exception {
        new JcrRepository(null, connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNoConnectionFactories() throws Exception {
        new JcrRepository(executionContextFactory, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNoDescriptorKey() {
        repository.getDescriptor(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyDescriptorKey() {
        repository.getDescriptor("");
    }

    @Test
    public void shouldProvideBuiltInDescriptorKeys() {
        testDescriptorKeys(repository);
    }

    @Test
    public void shouldProvideDescriptorValues() {
        testDescriptorValues(repository);
    }

    @Test
    public void shouldProvideBuiltInDescriptorsWhenNotSuppliedDescriptors() {
        Repository repository = new JcrRepository(descriptors, executionContextFactory, connectionFactory);
        testDescriptorKeys(repository);
        testDescriptorValues(repository);
    }

    @Test
    public void shouldProvideUserSuppliedDescriptors() {
        Map<String, String> descriptors = new HashMap<String, String>();
        descriptors.put("property", "value");
        Repository repository = new JcrRepository(descriptors, executionContextFactory, connectionFactory);
        testDescriptorKeys(repository);
        testDescriptorValues(repository);
        assertThat(repository.getDescriptor("property"), is("value"));
    }

    @Test
    public void shouldAllowLoginWithNoCredentials() throws Exception {
        Session session = repository.login();
        assertThat(session, notNullValue());
        session.logout();
        session = repository.login((Credentials)null);
        assertThat(session, notNullValue());
        session.logout();
        session = repository.login(null, JcrI18n.defaultWorkspaceName.text());
        assertThat(session, notNullValue());
    }

    @Test
    public void shouldAllowLoginWithProperCredentials() throws Exception {
        repository.login(credentials);
        stub(executionContextFactory.create(loginContext)).toReturn(executionContext);
        repository.login(new Credentials() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public LoginContext getLoginContext() throws LoginException {
                return loginContext;
            }
        });
    }

    @Test
    public void shouldAllowLoginWithNoWorkspaceName() throws Exception {
        Session session = repository.login((String)null);
        assertThat(session, notNullValue());
        session.logout();
        session = repository.login(credentials, null);
        assertThat(session, notNullValue());
        session.logout();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowLoginIfCredentialsDoNotProvideJaasMethod() throws Exception {
        repository.login(Mockito.mock(Credentials.class));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowLoginIfCredentialsReturnNoAccessControlContext() throws Exception {
        repository.login(new Credentials() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public AccessControlContext getAccessControlContext() {
                return null;
            }
        });
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowLoginIfCredentialsReturnNoLoginContext() throws Exception {
        repository.login(new Credentials() {

            private static final long serialVersionUID = 1L;

            @SuppressWarnings( "unused" )
            public LoginContext getLoginContext() {
                return null;
            }
        });
    }

    private void testDescriptorKeys( Repository repository ) {
        String[] keys = repository.getDescriptorKeys();
        assertThat(keys, notNullValue());
        assertThat(keys.length >= 15, is(true));
        assertThat(keys, hasItemInArray(Repository.LEVEL_1_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.LEVEL_2_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_LOCKING_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_OBSERVATION_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_QUERY_SQL_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_TRANSACTIONS_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.OPTION_VERSIONING_SUPPORTED));
        assertThat(keys, hasItemInArray(Repository.QUERY_XPATH_DOC_ORDER));
        assertThat(keys, hasItemInArray(Repository.QUERY_XPATH_POS_INDEX));
        assertThat(keys, hasItemInArray(Repository.REP_NAME_DESC));
        assertThat(keys, hasItemInArray(Repository.REP_VENDOR_DESC));
        assertThat(keys, hasItemInArray(Repository.REP_VENDOR_URL_DESC));
        assertThat(keys, hasItemInArray(Repository.REP_VERSION_DESC));
        assertThat(keys, hasItemInArray(Repository.SPEC_NAME_DESC));
        assertThat(keys, hasItemInArray(Repository.SPEC_VERSION_DESC));
    }

    private void testDescriptorValues( Repository repository ) {
        assertThat(repository.getDescriptor(Repository.LEVEL_1_SUPPORTED), is("true"));
        assertThat(repository.getDescriptor(Repository.LEVEL_2_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_LOCKING_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_QUERY_SQL_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_TRANSACTIONS_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.OPTION_VERSIONING_SUPPORTED), is("false"));
        assertThat(repository.getDescriptor(Repository.QUERY_XPATH_DOC_ORDER), is("true"));
        assertThat(repository.getDescriptor(Repository.QUERY_XPATH_POS_INDEX), is("true"));
        assertThat(repository.getDescriptor(Repository.REP_NAME_DESC), is(JcrI18n.REP_NAME_DESC.text()));
        assertThat(repository.getDescriptor(Repository.REP_VENDOR_DESC), is(JcrI18n.REP_VENDOR_DESC.text()));
        assertThat(repository.getDescriptor(Repository.REP_VENDOR_URL_DESC), is("http://www.jboss.org/dna"));
        assertThat(repository.getDescriptor(Repository.REP_VERSION_DESC), is("0.2"));
        assertThat(repository.getDescriptor(Repository.SPEC_NAME_DESC), is(JcrI18n.SPEC_NAME_DESC.text()));
        assertThat(repository.getDescriptor(Repository.SPEC_VERSION_DESC), is("1.0"));
    }
}
