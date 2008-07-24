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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import org.jboss.dna.spi.graph.connection.BasicExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositoryConnectionFactories;
import org.jboss.dna.spi.graph.connection.RepositorySourceListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedRepositoryTest {

    private ExecutionEnvironment env;
    private FederatedRepository repository;
    @Mock
    private FederatedRepositoryConfig config;
    @Mock
    private RepositorySourceListener listener1;
    @Mock
    private RepositorySourceListener listener2;
    @Mock
    private RepositoryConnectionFactories connectionFactories;

    // private BasicRepositoryConnectionPool connectionPool;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        env = new BasicExecutionEnvironment();
        repository = new FederatedRepository(env, connectionFactories, config);
    }

    @Test
    public void shouldHaveNoNameUponCreation() {
        assertThat(repository.getName(), is(nullValue()));
    }

    @Test
    public void shouldHaveNoListenersUponCreation() {
        assertThat(repository.getListeners(), is(notNullValue()));
        assertThat(repository.getListeners().isEmpty(), is(true));
    }

    @Test
    public void shouldNotAddNullListener() {
        assertThat(repository.addListener(null), is(false));
    }

    @Test
    public void shouldNotAddListenerIfAlreadyInList() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.getListeners().size(), is(1));
        assertThat(repository.addListener(listener1), is(false));
        assertThat(repository.getListeners().size(), is(1));
    }

    @Test
    public void shouldAddDifferentListeners() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.getListeners().size(), is(1));
        assertThat(repository.addListener(listener2), is(true));
        assertThat(repository.getListeners().size(), is(2));
        assertThat(repository.getListeners(), hasItems(listener1, listener2));
        assertThat(repository.getListeners().get(0), is(sameInstance(listener1)));
        assertThat(repository.getListeners().get(1), is(sameInstance(listener2)));
    }

    @Test
    public void shouldAllowReorderingOfListeners() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.addListener(listener2), is(true));
        assertThat(repository.getListeners().size(), is(2));
        assertThat(repository.getListeners(), hasItems(listener1, listener2));
        repository.getListeners().remove(0);
        repository.getListeners().add(1, listener1);
        assertThat(repository.getListeners(), hasItems(listener2, listener1));
        assertThat(repository.getListeners().get(0), is(sameInstance(listener2)));
        assertThat(repository.getListeners().get(1), is(sameInstance(listener1)));
    }

    @Test
    public void shouldAllowRemovalOfListeners() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.addListener(listener2), is(true));
        assertThat(repository.getListeners(), hasItems(listener1, listener2));
        assertThat(repository.removeListener(listener1), is(true));
        assertThat(repository.getListeners(), hasItems(listener2));
        assertThat(repository.removeListener(listener2), is(true));
        assertThat(repository.getListeners(), hasItems(new RepositorySourceListener[] {}));
    }

    @Test
    public void shouldNotRemoveListenerThatIsNotAlreadyRegistered() {
        assertThat(repository.getListeners().size(), is(0));
        assertThat(repository.addListener(listener1), is(true));
        assertThat(repository.getListeners().size(), is(1));
        assertThat(repository.removeListener(listener2), is(false));
    }

    @Test
    public void shouldHaveConfigurationAfterInitialization() {
        assertThat(repository.getConfiguration(), is(notNullValue()));
        assertThat(repository.getConfiguration(), is(sameInstance(config)));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowSettingConfigurationToNull() {
        repository.setConfiguration(null);
    }

}
