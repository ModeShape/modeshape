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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import org.jboss.dna.repository.util.ExecutionContext;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.impl.BasicNamespaceRegistry;
import org.jboss.dna.spi.graph.impl.StandardValueFactories;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederationServiceTest {

    private FederationService service;
    private FederatedRegion configRegion;
    private StandardValueFactories valueFactories;
    private PathFactory pathFactory;
    private String configSourceName;
    @Mock
    private ExecutionContext env;
    @Mock
    private RepositorySourceManager sources;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        configSourceName = "configSource";
        valueFactories = new StandardValueFactories(new BasicNamespaceRegistry());
        pathFactory = valueFactories.getPathFactory();
        Path pathInRepository = pathFactory.create("/dna:system");
        Path pathInSource = pathFactory.create("/some/root");
        configRegion = new FederatedRegion(pathInRepository, pathInSource, configSourceName);
        stub(env.getValueFactories()).toReturn(valueFactories);
        service = new FederationService(sources, configRegion, env, null);
    }

    @Ignore
    @Test
    public void shouldHaveServiceAdministratorAfterInstantiation() {
        assertThat(service.getAdministrator(), is(notNullValue()));
    }

    @Ignore
    @Test
    public void shouldHaveConfigurationRegionAfterInstantiation() {
        assertThat(service.getConfigurationRegion(), is(notNullValue()));
        assertThat(service.getConfigurationRegion(), is(sameInstance(configRegion)));
    }

    @Ignore
    @Test
    public void shouldHaveNonNullClassLoaderFactoryAfterInstantiatingWithNullClassLoaderFactoryReference() {
        assertThat(service.getClassLoaderFactory(), is(notNullValue()));
    }

}
