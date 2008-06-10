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
package org.jboss.dna.repository.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.jcr.AbstractJcrRepositoryTest;
import org.jboss.dna.common.naming.MockInitialContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class JndiSessionFactoryTest extends AbstractJcrRepositoryTest {

    public static final String MOCK_REPOSITORY_NAME = "java:jcr/unit/test/repository/1";
    public static final String MOCK_REPOSITORY_NAME_ALT = "java:jcr/unit/test/repository/2";

    private JndiSessionFactory factory;
    private Repository mockRepository;
    private Session session;

    @Before
    public void beforeEach() throws Exception {
        this.mockRepository = mock(Repository.class);
        MockInitialContext.register(MOCK_REPOSITORY_NAME, this.mockRepository);
        this.factory = new JndiSessionFactory();
    }

    @After
    public void afterEach() {
        if (session != null) {
            session.logout();
        }
        MockInitialContext.tearDown();
    }

    protected void assertNotRegistered( String name ) {
        try {
            new InitialContext().lookup(name);
            fail("Unexpectedly found registered object");
        } catch (NamingException e) {
            // expected ...
        }
    }

    protected void assertRegistered( String name,
                                     Object obj ) {
        try {
            assertThat(new InitialContext().lookup(name), is(sameInstance(obj)));
        } catch (NamingException e) {
            fail("Failed to find registered object \"" + name + "\"");
        }
    }

    @Test
    public void shouldCreateSessionForRegisteredRepository() {

    }

    @Test( expected = SystemFailureException.class )
    public void shouldThrowSystemFailureWhenUnableToFindRegisteredRepository() throws Exception {
        factory.createSession(MOCK_REPOSITORY_NAME + "something_extra_that_can't_be_found");
    }

    @Test
    public void shouldFindWorkspaceInRegisteredName() {
        assertThat(factory.getWorkspaceName("java:jcr/path/to/repository/workspaceName"), is("workspaceName"));
    }

    @Test
    public void shouldReturnNullWorkspaceIfRegisteredNameEndsInDelimiter() {
        assertThat(factory.getWorkspaceName("java:jcr/path/to/repository/"), is(nullValue()));
    }

    @Test
    public void shouldReturnNullWorkspaceIfRegisteredNameHasNoDelimiter() {
        assertThat(factory.getWorkspaceName("java:jcr"), is(nullValue()));
    }

    @Test
    public void shouldFindRepositoryInRegisteredName() {
        assertThat(factory.getRepositoryName("java:jcr/path/to/repository/workspaceName"), is("java:jcr/path/to/repository"));
    }

    @Test
    public void shouldConsiderWholeRegisteredNameToBeRepositoryNameIfRegisteredNameEndsInDelimiter() {
        assertThat(factory.getRepositoryName("java:jcr/path/to/repository/"), is("java:jcr/path/to/repository"));
    }

    @Test
    public void shouldConsiderWholeRegisteredNameToBeRepositoryNameIfRegisteredNameHasNoDelimiter() {
        assertThat(factory.getRepositoryName("java:jcr"), is("java:jcr"));
    }

    @Test
    public void shouldRegisterSuppliedRepositoryInJndi() {
        assertNotRegistered(MOCK_REPOSITORY_NAME_ALT);
        factory.registerRepository(MOCK_REPOSITORY_NAME_ALT, mockRepository);
        assertRegistered(MOCK_REPOSITORY_NAME_ALT, mockRepository);
    }

    @Test
    public void shouldUnregisterRepositoryInJndiIfNullRepositoryReference() {
        assertRegistered(MOCK_REPOSITORY_NAME, mockRepository);
        factory.registerRepository(MOCK_REPOSITORY_NAME, null);
        assertNotRegistered(MOCK_REPOSITORY_NAME);
    }

    @Test
    public void shouldRemoveAllTrailingDelimitersWhenRegisteringRepository() {
        assertNotRegistered("java:jcr/unit/test/repository");
        factory.registerRepository("java:jcr/unit/test/repository///", mockRepository);
        assertRegistered("java:jcr/unit/test/repository", mockRepository);
    }

    @Test
    public void shouldCreateAnonymousSessionInRepositoryIfNoCredentialsAreRegisterd() throws Exception {
        Repository repository = getRepository();
        factory.registerRepository("java:jcr/unit/test/repository/", repository);
        session = factory.createSession("java:jcr/unit/test/repository/default");
        assertThat(session, is(notNullValue()));
        assertThat(session.getUserID(), is("anonymous")); // as defined in the Jackrabbit configuration file
    }

    @Test
    public void shouldCreateNonAnonymousSessionInRepositoryIfCredentialsAreRegistered() throws Exception {
        Repository repository = getRepository();
        factory.registerRepository("java:jcr/unit/test/repository/", repository);
        factory.registerCredentials("java:jcr/unit/test/repository/default", "jsmith", "secret".toCharArray());
        session = factory.createSession("java:jcr/unit/test/repository/default");
        assertThat(session, is(notNullValue()));
        assertThat(session.getUserID(), is("jsmith")); // as defined in the Jackrabbit configuration file
    }
}
