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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.connection.ExecutionEnvironment;
import org.jboss.dna.spi.graph.connection.RepositorySource;
import org.jboss.dna.spi.graph.impl.BasicNamespaceRegistry;
import org.jboss.dna.spi.graph.impl.BasicPropertyFactory;
import org.jboss.dna.spi.graph.impl.StandardValueFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederationServiceTest {

    public static final String CLASSNAME = FederationService.CLASSNAME_PROPERTY_NAME;
    public static final String CLASSPATH = FederationService.CLASSPATH_PROPERTY_NAME;
    public static final String PATH_IN_REPOSITORY = FederationService.REGION_PATH_IN_REPOSITORY_PROPERTY_NAME;
    public static final String PATH_IN_SOURCE = FederationService.REGION_PATH_IN_SOURCE_PROPERTY_NAME;
    public static final String SOURCE_NAME = FederationService.REGION_SOURCE_NAME;
    public static final String TIME_TO_EXPIRE = FederationService.CACHE_POLICY_TIME_TO_EXPIRE;
    public static final String TIME_TO_CACHE = FederationService.CACHE_POLICY_TIME_TO_CACHE;

    private FederationService service;
    private FederatedRegion configRegion;
    private StandardValueFactories valueFactories;
    private PropertyFactory propertyFactory;
    private PathFactory pathFactory;
    private String configSourceName;
    private SimpleRepository configRepository;
    private SimpleRepositorySource configRepositorySource;
    @Mock
    private ExecutionEnvironment env;
    @Mock
    private RepositorySourceManager sources;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        NamespaceRegistry registry = new BasicNamespaceRegistry();
        registry.register("dna", "http://www.jboss.org/dna");
        valueFactories = new StandardValueFactories(registry);
        pathFactory = valueFactories.getPathFactory();
        propertyFactory = new BasicPropertyFactory(valueFactories);
        Path pathInRepository = pathFactory.create("/");
        Path pathInSource = pathFactory.create("/reposX");
        configSourceName = "configSource";
        configRegion = new FederatedRegion("config region", pathInRepository, pathInSource, configSourceName);
        configRepository = new SimpleRepository("Configuration Repository");
        configRepositorySource = new SimpleRepositorySource();
        configRepositorySource.setRepositoryName(configRepository.getRepositoryName());
        configRepositorySource.setName(configSourceName);
        stub(sources.getConnectionFactory(configSourceName)).toReturn(configRepositorySource);
        stub(env.getValueFactories()).toReturn(valueFactories);
        stub(env.getPropertyFactory()).toReturn(propertyFactory);
        stub(env.getNamespaceRegistry()).toReturn(registry);
        service = new FederationService(sources, configRegion, env, null);
    }

    @After
    public void afterEach() throws Exception {
        service.getAdministrator().shutdown();
        service.getAdministrator().awaitTermination(4, TimeUnit.SECONDS);
        SimpleRepository.shutdownAll();
        Logger.getLogger(getClass()).trace("");
    }

    @Test
    public void shouldHaveServiceAdministratorAfterInstantiation() {
        assertThat(service.getAdministrator(), is(notNullValue()));
    }

    @Test
    public void shouldHaveConfigurationRegionAfterInstantiation() {
        assertThat(service.getConfigurationRegion(), is(notNullValue()));
        assertThat(service.getConfigurationRegion(), is(sameInstance(configRegion)));
    }

    @Test
    public void shouldHaveAnExecutionEnvironmentAfterInstantiation() {
        assertThat(service.getExecutionEnvironment(), is(notNullValue()));
        assertThat(service.getExecutionEnvironment(), is(sameInstance(env)));
    }

    @Test
    public void shouldHaveNonNullClassLoaderFactoryAfterInstantiatingWithNullClassLoaderFactoryReference() {
        assertThat(service.getClassLoaderFactory(), is(notNullValue()));
    }

    @Test
    public void shouldHaveNonNullClassLoaderFactoryAfterInstantiatingWithClassLoaderFactoryReference() {
        ClassLoaderFactory classLoaderFactory = mock(ClassLoaderFactory.class);
        service = new FederationService(sources, configRegion, env, classLoaderFactory);
        assertThat(service.getClassLoaderFactory(), is(notNullValue()));
        assertThat(service.getClassLoaderFactory(), is(sameInstance(classLoaderFactory)));
    }

    @Test
    public void shouldHaveNullJndiNameAfterInstantiation() {
        assertThat(service.getJndiName(), is(nullValue()));
    }

    @Test
    public void shouldAllowShuttingDownBeforeStartingUp() throws Exception {
        assertThat(service.getAdministrator().getState(), is(ServiceAdministrator.State.PAUSED));
        service.getAdministrator().shutdown();
        service.getAdministrator().awaitTermination(1, TimeUnit.SECONDS);
        assertThat(service.getAdministrator().getState(), is(ServiceAdministrator.State.TERMINATED));
    }

    @Test( expected = FederationException.class )
    public void shouldFailToStartUpIfConfigurationRepositorySourceIsNotFound() {
        stub(sources.getConnectionFactory(configSourceName)).toReturn(null);
        service.getAdministrator().start();
    }

    @Test( expected = FederationException.class )
    public void shouldFailToStartUpIfUnableToConnectToConfigurationRepository() throws Exception {
        RepositorySource mockSource = mock(RepositorySource.class);
        stub(sources.getConnectionFactory(configSourceName)).toReturn(mockSource);
        stub(mockSource.getConnection()).toThrow(new UnsupportedOperationException());
        service.getAdministrator().start();
    }

    @Test( expected = FederationException.class )
    public void shouldFailToStartUpIfInterruptedWhileConnectingToConfigurationRepository() throws Exception {
        RepositorySource mockSource = mock(RepositorySource.class);
        stub(sources.getConnectionFactory(configSourceName)).toReturn(mockSource);
        stub(mockSource.getConnection()).toThrow(new InterruptedException());
        service.getAdministrator().start();
    }

    @Test
    public void shouldStartUpUsingConfigurationRepositoryThatContainsSomeSources() {
        // Use a real source manager for this test ...
        sources = new RepositorySourceManager(sources);
        sources.addSource(configRepositorySource, true);
        assertThat(sources.getSources(), hasItems((RepositorySource)configRepositorySource));
        assertThat(sources.getSources().size(), is(1));
        service = new FederationService(sources, configRegion, env, null);

        // Set up the configuration repository to contain 3 sources ...
        configRepository.create(env, "/reposX/dna:sources");
        configRepository.setProperty(env, "/reposX/dna:sources/source A", CLASSNAME, SimpleRepositorySource.class.getName());
        configRepository.setProperty(env, "/reposX/dna:sources/source A", CLASSPATH, "");
        configRepository.setProperty(env, "/reposX/dna:sources/source A", "repositoryName", "sourceReposA");
        configRepository.setProperty(env, "/reposX/dna:sources/source A", "retryLimit", 3);

        configRepository.setProperty(env, "/reposX/dna:sources/source B", CLASSNAME, SimpleRepositorySource.class.getName());
        configRepository.setProperty(env, "/reposX/dna:sources/source B", CLASSPATH, "");
        configRepository.setProperty(env, "/reposX/dna:sources/source B", "repositoryName", "sourceReposB");

        configRepository.setProperty(env, "/reposX/dna:sources/source C", CLASSNAME, SimpleRepositorySource.class.getName());
        configRepository.setProperty(env, "/reposX/dna:sources/source C", CLASSPATH, "");
        configRepository.setProperty(env, "/reposX/dna:sources/source C", "repositoryName", "sourceReposC");

        // Now, start up the service ...
        service.getAdministrator().start();

        // and verify that the sources were added to the manager...
        assertThat(sources.getSources().size(), is(4));
        assertThat(sources.getConnectionFactory("source A"), is(instanceOf(SimpleRepositorySource.class)));
        assertThat(sources.getConnectionFactory("source B"), is(instanceOf(SimpleRepositorySource.class)));
        assertThat(sources.getConnectionFactory("source C"), is(instanceOf(SimpleRepositorySource.class)));

        SimpleRepositorySource sourceA = (SimpleRepositorySource)sources.getConnectionFactory("source A");
        assertThat(sourceA.getName(), is("source A"));
        assertThat(sourceA.getRepositoryName(), is("sourceReposA"));
        assertThat(sourceA.getRetryLimit(), is(3));

        SimpleRepositorySource sourceB = (SimpleRepositorySource)sources.getConnectionFactory("source B");
        assertThat(sourceB.getName(), is("source B"));
        assertThat(sourceB.getRepositoryName(), is("sourceReposB"));
        assertThat(sourceB.getRetryLimit(), is(SimpleRepositorySource.DEFAULT_RETRY_LIMIT));

        SimpleRepositorySource sourceC = (SimpleRepositorySource)sources.getConnectionFactory("source C");
        assertThat(sourceC.getName(), is("source C"));
        assertThat(sourceC.getRepositoryName(), is("sourceReposC"));
        assertThat(sourceC.getRetryLimit(), is(SimpleRepositorySource.DEFAULT_RETRY_LIMIT));
    }

    @Test
    public void shouldStartUpUsingConfigurationRepositoryThatContainsNoSources() {
        // Set up the configuration repository to contain NO sources ...
        configRepository.create(env, "/reposX/dna:sources");

        // Now, start up the service ...
        service.getAdministrator().start();

        // and verify that the configuration source was obtained from the manager ...
        verify(sources, times(1)).getConnectionFactory(configSourceName);

        // and verify that the sources were never added to the manager...
        verifyNoMoreInteractions(sources);
    }

    @Test
    public void shouldStartUpAndCreateRepositoryUsingConfigurationRepositoryThatContainsNoSources() {
        // Set up the configuration repository ...
        configRepository.create(env, "/reposX/dna:sources");
        configRepository.setProperty(env, "/reposX/dna:sources/source A", CLASSNAME, SimpleRepositorySource.class.getName());
        configRepository.setProperty(env, "/reposX/dna:sources/source A", CLASSPATH, "");
        configRepository.setProperty(env, "/reposX/dna:sources/source A", "repositoryName", "sourceReposA");
        configRepository.setProperty(env, "/reposX/dna:sources/source A", "retryLimit", 3);

        String fedReposPath = "/reposX/dna:repositories/fed repos/";
        configRepository.setProperty(env, fedReposPath, TIME_TO_CACHE, "10000");
        configRepository.setProperty(env, fedReposPath, TIME_TO_EXPIRE, "20000");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region1", PATH_IN_REPOSITORY, "/a/b/c");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region1", PATH_IN_SOURCE, "/sx/sy");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region1", SOURCE_NAME, "source A");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region2", PATH_IN_REPOSITORY, "/");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region2", PATH_IN_SOURCE, "/");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region2", SOURCE_NAME, "source B");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region3", PATH_IN_REPOSITORY, "/d/e/f");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region3", PATH_IN_SOURCE, "/");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region3", SOURCE_NAME, "source C");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region4", PATH_IN_REPOSITORY, "/");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region4", PATH_IN_SOURCE, "/x/y/z");
        configRepository.setProperty(env, fedReposPath + "dna:regions/region4", SOURCE_NAME, "source A");

        // Now, start up the service ...
        service.getAdministrator().start();

        // Create the repository ...
        FederatedRepository repository = service.getRepository("fed repos");
        assertThat(repository, is(notNullValue()));
        assertThat(repository.getName(), is("fed repos"));
        assertThat(repository.getConfiguration().getDefaultCachePolicy().getTimeToCache(), is(10000l));
        assertThat(repository.getConfiguration().getDefaultCachePolicy().getTimeToExpire(), is(20000l));
        assertThat(repository.getConfiguration().getRegions().get(0).getRegionName(), is("region1"));
        assertThat(repository.getConfiguration().getRegions().get(0).getPathInRepository(), is(pathFactory.create("/a/b/c")));
        assertThat(repository.getConfiguration().getRegions().get(0).getPathInSource(), is(pathFactory.create("/sx/sy")));
        assertThat(repository.getConfiguration().getRegions().get(0).getSourceName(), is("source A"));
        assertThat(repository.getConfiguration().getRegions().get(1).getRegionName(), is("region2"));
        assertThat(repository.getConfiguration().getRegions().get(1).getPathInRepository(), is(pathFactory.create("/")));
        assertThat(repository.getConfiguration().getRegions().get(1).getPathInSource(), is(pathFactory.create("/")));
        assertThat(repository.getConfiguration().getRegions().get(1).getSourceName(), is("source B"));
        assertThat(repository.getConfiguration().getRegions().get(2).getRegionName(), is("region3"));
        assertThat(repository.getConfiguration().getRegions().get(2).getPathInRepository(), is(pathFactory.create("/d/e/f")));
        assertThat(repository.getConfiguration().getRegions().get(2).getPathInSource(), is(pathFactory.create("/")));
        assertThat(repository.getConfiguration().getRegions().get(2).getSourceName(), is("source C"));
        assertThat(repository.getConfiguration().getRegions().get(3).getRegionName(), is("region4"));
        assertThat(repository.getConfiguration().getRegions().get(3).getPathInRepository(), is(pathFactory.create("/")));
        assertThat(repository.getConfiguration().getRegions().get(3).getPathInSource(), is(pathFactory.create("/x/y/z")));
        assertThat(repository.getConfiguration().getRegions().get(3).getSourceName(), is("source A"));

        for (int i = 0; i != 10; ++i) {
            assertThat(service.getRepository("fed repos"), is(sameInstance(repository)));
        }
    }

    @Test( expected = FederationException.class )
    public void shouldThrowExceptionWhenGettingRepositoryThatDoesNotExistInConfigurationRepository() {
        // No sources, no repositories in configuration repository ...

        // Now, start up the service ...
        service.getAdministrator().start();

        // Get the non-existant repository ...
        service.getRepository("non-existant repos");
    }
}
