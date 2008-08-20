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
package org.jboss.dna.repository;

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
import org.jboss.dna.connector.federation.FederationException;
import org.jboss.dna.connector.federation.Projection;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.spi.DnaLexicon;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.connector.BasicExecutionContext;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.connector.RepositorySource;
import org.jboss.dna.spi.connector.SimpleRepository;
import org.jboss.dna.spi.connector.SimpleRepositorySource;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class RepositoryServiceTest {

    public static final String CLASSNAME = RepositoryService.CLASSNAME_PROPERTY_NAME;
    public static final String CLASSPATH = RepositoryService.CLASSPATH_PROPERTY_NAME;
    public static final String PROJECTION_RULES = RepositoryService.PROJECTION_RULES_PROPERTY_NAME;
    public static final String TIME_TO_EXPIRE = RepositoryService.CACHE_POLICY_TIME_TO_EXPIRE;
    public static final String TIME_TO_CACHE = RepositoryService.CACHE_POLICY_TIME_TO_CACHE;

    private RepositoryService service;
    private Projection configProjection;
    private PathFactory pathFactory;
    private String configSourceName;
    private SimpleRepository configRepository;
    private SimpleRepositorySource configRepositorySource;
    private RepositoryConnection configRepositoryConnection;
    private ExecutionContext context;
    @Mock
    private RepositorySourceManager sources;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new BasicExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        pathFactory = context.getValueFactories().getPathFactory();
        Path pathInRepository = pathFactory.create("/");
        Path pathInSource = pathFactory.create("/reposX");
        configSourceName = "configSource";
        Projection.Rule configProjectionRule = new Projection.PathRule(pathInRepository, pathInSource);
        configProjection = new Projection(configSourceName, configProjectionRule);
        configRepository = SimpleRepository.get("Configuration Repository");
        configRepositorySource = new SimpleRepositorySource();
        configRepositorySource.setRepositoryName(configRepository.getRepositoryName());
        configRepositorySource.setName(configSourceName);
        configRepositoryConnection = configRepositorySource.getConnection();
        stub(sources.createConnection(configSourceName)).toReturn(configRepositoryConnection);
        service = new RepositoryService(sources, configProjection, context, null);
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
        assertThat(service.getConfigurationProjection(), is(notNullValue()));
        assertThat(service.getConfigurationProjection(), is(sameInstance(configProjection)));
    }

    @Test
    public void shouldHaveAnExecutionEnvironmentAfterInstantiation() {
        assertThat(service.getExecutionEnvironment(), is(notNullValue()));
        assertThat(service.getExecutionEnvironment(), is(sameInstance(context)));
    }

    @Test
    public void shouldHaveNonNullClassLoaderFactoryAfterInstantiatingWithNullClassLoaderFactoryReference() {
        assertThat(service.getClassLoaderFactory(), is(notNullValue()));
    }

    @Test
    public void shouldHaveNonNullClassLoaderFactoryAfterInstantiatingWithClassLoaderFactoryReference() {
        ClassLoaderFactory classLoaderFactory = mock(ClassLoaderFactory.class);
        service = new RepositoryService(sources, configProjection, context, classLoaderFactory);
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
    public void shouldFailToStartUpIfConfigurationRepositorySourceIsNotFound() throws Exception {
        stub(sources.createConnection(configSourceName)).toReturn(null);
        service.getAdministrator().start();
    }

    @Test( expected = FederationException.class )
    public void shouldFailToStartUpIfUnableToConnectToConfigurationRepository() throws Exception {
        stub(sources.createConnection(configSourceName)).toThrow(new UnsupportedOperationException());
        service.getAdministrator().start();
    }

    @Test( expected = FederationException.class )
    public void shouldFailToStartUpIfInterruptedWhileConnectingToConfigurationRepository() throws Exception {
        stub(sources.createConnection(configSourceName)).toThrow(new InterruptedException());
        service.getAdministrator().start();
    }

    @Test
    public void shouldStartUpUsingConfigurationRepositoryThatContainsSomeSources() throws Exception {
        // Use a real source manager for this test ...
        sources = new RepositorySourceManager(sources);
        sources.addSource(configRepositorySource);
        assertThat(sources.getSources(), hasItems((RepositorySource)configRepositorySource));
        assertThat(sources.getSources().size(), is(1));
        service = new RepositoryService(sources, configProjection, context, null);

        // Set up the configuration repository to contain 3 sources ...
        configRepository.create(context, "/reposX/dna:sources");
        configRepository.setProperty(context, "/reposX/dna:sources/source A", CLASSNAME, SimpleRepositorySource.class.getName());
        configRepository.setProperty(context, "/reposX/dna:sources/source A", CLASSPATH, "");
        configRepository.setProperty(context, "/reposX/dna:sources/source A", "repositoryName", "sourceReposA");
        configRepository.setProperty(context, "/reposX/dna:sources/source A", "retryLimit", 3);

        configRepository.setProperty(context, "/reposX/dna:sources/source B", CLASSNAME, SimpleRepositorySource.class.getName());
        configRepository.setProperty(context, "/reposX/dna:sources/source B", CLASSPATH, "");
        configRepository.setProperty(context, "/reposX/dna:sources/source B", "repositoryName", "sourceReposB");

        configRepository.setProperty(context, "/reposX/dna:sources/source C", CLASSNAME, SimpleRepositorySource.class.getName());
        configRepository.setProperty(context, "/reposX/dna:sources/source C", CLASSPATH, "");
        configRepository.setProperty(context, "/reposX/dna:sources/source C", "repositoryName", "sourceReposC");

        // Now, start up the service ...
        service.getAdministrator().start();

        // and verify that the sources were added to the manager...
        assertThat(sources.getSources().size(), is(4));
        assertThat(sources.getSource("source A"), is(instanceOf(SimpleRepositorySource.class)));
        assertThat(sources.getSource("source B"), is(instanceOf(SimpleRepositorySource.class)));
        assertThat(sources.getSource("source C"), is(instanceOf(SimpleRepositorySource.class)));

        SimpleRepositorySource sourceA = (SimpleRepositorySource)sources.getSource("source A");
        assertThat(sourceA.getName(), is("source A"));
        assertThat(sourceA.getRepositoryName(), is("sourceReposA"));
        assertThat(sourceA.getRetryLimit(), is(3));

        SimpleRepositorySource sourceB = (SimpleRepositorySource)sources.getSource("source B");
        assertThat(sourceB.getName(), is("source B"));
        assertThat(sourceB.getRepositoryName(), is("sourceReposB"));
        assertThat(sourceB.getRetryLimit(), is(SimpleRepositorySource.DEFAULT_RETRY_LIMIT));

        SimpleRepositorySource sourceC = (SimpleRepositorySource)sources.getSource("source C");
        assertThat(sourceC.getName(), is("source C"));
        assertThat(sourceC.getRepositoryName(), is("sourceReposC"));
        assertThat(sourceC.getRetryLimit(), is(SimpleRepositorySource.DEFAULT_RETRY_LIMIT));
    }

    @Test
    public void shouldStartUpUsingConfigurationRepositoryThatContainsNoSources() throws Exception {
        // Set up the configuration repository to contain NO sources ...
        configRepository.create(context, "/reposX/dna:sources");

        // Now, start up the service ...
        service.getAdministrator().start();

        // and verify that the configuration source was obtained from the manager ...
        verify(sources, times(1)).createConnection(configSourceName); // once for checking source, second for getting

        // and verify that the sources were never added to the manager...
        verifyNoMoreInteractions(sources);
    }

    @Test
    public void shouldStartUpAndCreateRepositoryUsingConfigurationRepositoryThatContainsNoSources() {
        // Set up the configuration repository ...
        configRepository.create(context, "/reposX/dna:sources");
        configRepository.setProperty(context, "/reposX/dna:sources/source A", CLASSNAME, SimpleRepositorySource.class.getName());
        configRepository.setProperty(context, "/reposX/dna:sources/source A", CLASSPATH, "");
        configRepository.setProperty(context, "/reposX/dna:sources/source A", "repositoryName", "sourceReposA");
        configRepository.setProperty(context, "/reposX/dna:sources/source A", "retryLimit", 3);

        String fedReposPath = "/reposX/dna:repositories/fed repos/";
        configRepository.setProperty(context, fedReposPath, TIME_TO_CACHE, "10000");
        configRepository.setProperty(context, fedReposPath, TIME_TO_EXPIRE, "20000");
        configRepository.setProperty(context, fedReposPath + "dna:regions/source A", PROJECTION_RULES, "/a/b/c => /sx/sy");
        configRepository.setProperty(context, fedReposPath + "dna:regions/source B", PROJECTION_RULES, "/ => /");
        configRepository.setProperty(context, fedReposPath + "dna:regions/source C", PROJECTION_RULES, "/d/e/f => /");
        configRepository.setProperty(context, fedReposPath + "dna:regions/source D", PROJECTION_RULES, "/ => /x/y/z");

        // Now, start up the service ...
        service.getAdministrator().start();

        // // Create the repository ...
        // FederatedRepositorySource repository = (FederatedRepositorySource)sources.getConnectionFactory("fed repos");
        // assertThat(repository, is(notNullValue()));
        // assertThat(repository.getName(), is("fed repos"));
        // assertThat(repository.getDefaultCachePolicy().getTimeToCache(), is(10000l));
        // assertThat(repository.getDefaultCachePolicy().getTimeToExpire(), is(20000l));
        // assertThat(repository.getCacheProjection(), is(sameInstance(configProjection)));
        // assertThat(repository.getConfiguration().getSourceProjections().get(0).getPathsInSource(pathFactory.create("/a/b/c"),
        // pathFactory),
        // hasItems(pathFactory.create("/sx/sy")));
        // assertThat(repository.getConfiguration().getSourceProjections().get(0).getSourceName(), is("source A"));
        // assertThat(repository.getConfiguration().getSourceProjections().get(1).getPathsInSource(pathFactory.create("/"),
        // pathFactory),
        // hasItems(pathFactory.create("/")));
        // assertThat(repository.getConfiguration().getSourceProjections().get(1).getSourceName(), is("source B"));
        // assertThat(repository.getConfiguration().getSourceProjections().get(2).getPathsInSource(pathFactory.create("/d/e/f"),
        // pathFactory),
        // hasItems(pathFactory.create("/")));
        // assertThat(repository.getConfiguration().getSourceProjections().get(2).getSourceName(), is("source C"));
        // assertThat(repository.getConfiguration().getSourceProjections().get(3).getPathsInSource(pathFactory.create("/"),
        // pathFactory),
        // hasItems(pathFactory.create("/x/y/z")));
        // assertThat(repository.getConfiguration().getSourceProjections().get(3).getSourceName(), is("source A"));
        //
        // for (int i = 0; i != 10; ++i) {
        // assertThat(service.getRepository("fed repos"), is(sameInstance(repository)));
        // }
    }

}
