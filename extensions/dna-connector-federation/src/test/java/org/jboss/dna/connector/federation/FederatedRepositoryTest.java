/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.ArrayList;
import java.util.Collection;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatedRepositoryTest {

    private String name;
    private ExecutionContext context;
    private FederatedRepository repository;
    @Mock
    private FederatedWorkspace config;
    @Mock
    private RepositoryConnectionFactory connectionFactory;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        name = "Repository";
        context = new ExecutionContext();
        stub(config.getName()).toReturn("workspace");
        Collection<FederatedWorkspace> configs = new ArrayList<FederatedWorkspace>();
        configs.add(config);
        repository = new FederatedRepository(name, context, connectionFactory, configs);
    }

    @Test
    public void shouldHaveNameUponCreation() {
        assertThat(repository.getName(), is(name));
    }

    @Test
    public void shouldHaveConfigurationAfterInitialization() {
        assertThat(repository.getWorkspaceConfigurations(), is(notNullValue()));
        assertThat(repository.getWorkspaceConfigurations().get("workspace"), is(sameInstance(config)));
    }

}
