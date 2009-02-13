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
package org.jboss.dna.repository;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.connector.federation.FederationException;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.repository.service.ServiceAdministrator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class RepositoryServiceTest {

    private RepositoryService service;
    private String configSourceName;
    private String configWorkspaceName;
    private Graph configRepository;
    private InMemoryRepositorySource configRepositorySource;
    private ExecutionContext context;
    private Path root;
    @Mock
    private RepositoryLibrary sources;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        configSourceName = "configSource";
        configWorkspaceName = null;
        configRepositorySource = new InMemoryRepositorySource();
        configRepositorySource.setName(configSourceName);
        configRepositorySource.setDefaultWorkspaceName("default");
        configRepository = Graph.create(configRepositorySource, context);
        RepositoryConnection configRepositoryConnection = configRepositorySource.getConnection();
        stub(sources.createConnection(configSourceName)).toReturn(configRepositoryConnection);
        root = context.getValueFactories().getPathFactory().createRootPath();
        service = new RepositoryService(sources, configSourceName, configWorkspaceName, context);
    }

    @After
    public void afterEach() throws Exception {
        service.getAdministrator().shutdown();
        service.getAdministrator().awaitTermination(4, TimeUnit.SECONDS);
        Logger.getLogger(getClass()).trace("");
    }

    @Test
    public void shouldHaveServiceAdministratorAfterInstantiation() {
        assertThat(service.getAdministrator(), is(notNullValue()));
    }

    @Test
    public void shouldHaveConfigurationSourceAfterInstantiation() {
        assertThat(service.getConfigurationSourceName(), is(notNullValue()));
        assertThat(service.getConfigurationSourceName(), is(configSourceName));
    }

    @Test
    public void shouldHaveAnExecutionEnvironmentAfterInstantiation() {
        assertThat(service.getExecutionEnvironment(), is(notNullValue()));
        assertThat(service.getExecutionEnvironment(), is(sameInstance(context)));
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

    @Test
    public void shouldStartUpUsingConfigurationRepositoryThatContainsSomeSources() throws Exception {
        // Use a real source manager for this test ...
        sources = new RepositoryLibrary(sources);
        sources.addSource(configRepositorySource);
        assertThat(sources.getSources(), hasItems((RepositorySource)configRepositorySource));
        assertThat(sources.getSources().size(), is(1));
        service = new RepositoryService(sources, configSourceName, configWorkspaceName, root, context);

        // Set up the configuration repository to contain 3 sources ...
        final String className = InMemoryRepositorySource.class.getName();
        configRepository.create("/dna:sources");
        configRepository.create("/dna:sources/source A");
        configRepository.set(DnaLexicon.CLASSNAME).on("/dna:sources/source A").to(className);
        configRepository.set(DnaLexicon.CLASSPATH).on("/dna:sources/source A").to("");
        configRepository.set("retryLimit").on("/dna:sources/source A").to(3);

        configRepository.create("/dna:sources/source B");
        configRepository.set(DnaLexicon.CLASSNAME).on("/dna:sources/source B").to(className);
        configRepository.set(DnaLexicon.CLASSPATH).on("/dna:sources/source B").to("");

        configRepository.create("/dna:sources/source C");
        configRepository.set(DnaLexicon.CLASSNAME).on("/dna:sources/source C").to(className);
        configRepository.set(DnaLexicon.CLASSPATH).on("/dna:sources/source C").to("");

        // Now, start up the service ...
        service.getAdministrator().start();

        // and verify that the sources were added to the manager...
        assertThat(sources.getSources().size(), is(4));
        assertThat(sources.getSource("source A"), is(instanceOf(InMemoryRepositorySource.class)));
        assertThat(sources.getSource("source B"), is(instanceOf(InMemoryRepositorySource.class)));
        assertThat(sources.getSource("source C"), is(instanceOf(InMemoryRepositorySource.class)));

        InMemoryRepositorySource sourceA = (InMemoryRepositorySource)sources.getSource("source A");
        assertThat(sourceA.getName(), is("source A"));
        assertThat(sourceA.getRetryLimit(), is(3));

        InMemoryRepositorySource sourceB = (InMemoryRepositorySource)sources.getSource("source B");
        assertThat(sourceB.getName(), is("source B"));
        assertThat(sourceB.getRetryLimit(), is(InMemoryRepositorySource.DEFAULT_RETRY_LIMIT));

        InMemoryRepositorySource sourceC = (InMemoryRepositorySource)sources.getSource("source C");
        assertThat(sourceC.getName(), is("source C"));
        assertThat(sourceC.getRetryLimit(), is(InMemoryRepositorySource.DEFAULT_RETRY_LIMIT));
    }

    @Test
    public void shouldStartUpUsingConfigurationRepositoryThatContainsNoSources() throws Exception {
        // Set up the configuration repository to contain NO sources ...
        configRepository.create("/dna:sources");

        // Now, start up the service ...
        service.getAdministrator().start();

        // and verify that the configuration source was obtained from the manager ...
        verify(sources, times(2)).createConnection(configSourceName); // once for checking source, second for getting

        // and verify that the sources were never added to the manager...
        verifyNoMoreInteractions(sources);
    }

    @Test
    public void shouldStartUpAndCreateRepositoryUsingConfigurationRepositoryThatContainsNoSources() {
        // Set up the configuration repository ...
        configRepository.useWorkspace("default");
        configRepository.create("/dna:sources");
        configRepository.create("/dna:sources/source A");

        final String className = InMemoryRepositorySource.class.getName();
        configRepository.set(DnaLexicon.CLASSNAME).on("/dna:sources/source A").to(className);
        configRepository.set(DnaLexicon.CLASSPATH).on("/dna:sources/source A").to("");
        configRepository.set("retryLimit").on("/dna:sources/source A").to(3);

        String fedReposPath = "/dna:repositories/fed repos/";
        configRepository.create("/dna:repositories");
        configRepository.create("/dna:repositories/fed repos");
        configRepository.create("/dna:repositories/fed repos/dna:regions");
        configRepository.create("/dna:repositories/fed repos/dna:regions/source A");
        configRepository.create("/dna:repositories/fed repos/dna:regions/source B");
        configRepository.create("/dna:repositories/fed repos/dna:regions/source C");
        configRepository.create("/dna:repositories/fed repos/dna:regions/source D");
        configRepository.set(DnaLexicon.TIME_TO_EXPIRE).on(fedReposPath).to(20000);
        configRepository.set(DnaLexicon.PROJECTION_RULES).on(fedReposPath + "dna:regions/source A").to("/a/b/c => /sx/sy");
        configRepository.set(DnaLexicon.PROJECTION_RULES).on(fedReposPath + "dna:regions/source B").to("/ => /");
        configRepository.set(DnaLexicon.PROJECTION_RULES).on(fedReposPath + "dna:regions/source C").to("/d/e/f => /");
        configRepository.set(DnaLexicon.PROJECTION_RULES).on(fedReposPath + "dna:regions/source D").to("/ => /x/y/z");

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

    @Test
    public void shouldConfigureRepositorySourceWithSetterThatTakesArrayButWithSingleValues() {
        RepositoryLibrary sources = new RepositoryLibrary(context);
        sources.addSource(configRepositorySource);
        service = new RepositoryService(sources, configSourceName, configWorkspaceName, context);

        // Set up the configuration repository ...
        configRepository.useWorkspace("default");
        configRepository.create("/dna:system");
        configRepository.create("/dna:system/dna:sources");
        configRepository.create("/dna:system/dna:sources/source A");

        final String className = FakeRepositorySource.class.getName();
        configRepository.set(DnaLexicon.CLASSNAME).on("/dna:system/dna:sources/source A").to(className);
        configRepository.set(DnaLexicon.CLASSPATH).on("/dna:system/dna:sources/source A").to("");
        configRepository.set("retryLimit").on("/dna:system/dna:sources/source A").to(3);
        configRepository.set("intParam").on("/dna:system/dna:sources/source A").to("3");
        configRepository.set("shortParam").on("/dna:system/dna:sources/source A").to("32");
        configRepository.set("booleanParam").on("/dna:system/dna:sources/source A").to("true");
        configRepository.set("stringParam").on("/dna:system/dna:sources/source A").to("string value");
        configRepository.set("intArrayParam").on("/dna:system/dna:sources/source A").to("3");
        configRepository.set("booleanArrayParam").on("/dna:system/dna:sources/source A").to("true");
        configRepository.set("longObjectArrayParam").on("/dna:system/dna:sources/source A").to("987654321");
        configRepository.set("booleanObjectArrayParam").on("/dna:system/dna:sources/source A").to("true");
        configRepository.set("stringArrayParam").on("/dna:system/dna:sources/source A").to("string value");

        // Now, start up the service ...
        service.getAdministrator().start();

        // Get the source, which should be configured ...
        RepositorySource repositorySourceA = service.getRepositorySourceManager().getSource("source A");
        assertThat(repositorySourceA, is(instanceOf(FakeRepositorySource.class)));
        FakeRepositorySource sourceA = (FakeRepositorySource)repositorySourceA;

        assertThat(sourceA.getIntParam(), is(3));
        assertThat(sourceA.getShortParam(), is((short)32));
        assertThat(sourceA.isBooleanParam(), is(true));
        assertThat(sourceA.getStringParam(), is("string value"));
        assertThat(sourceA.getIntArrayParam(), is(new int[] {3}));
        assertThat(sourceA.getBooleanArrayParam(), is(new boolean[] {true}));
        assertThat(sourceA.getLongObjectArrayParam(), is(new Long[] {987654321L}));
        assertThat(sourceA.getBooleanObjectArrayParam(), is(new Boolean[] {Boolean.TRUE}));
        assertThat(sourceA.getStringArrayParam(), is(new String[] {"string value"}));
    }

}
