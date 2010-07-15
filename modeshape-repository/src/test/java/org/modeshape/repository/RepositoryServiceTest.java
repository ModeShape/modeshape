/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.repository;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.observe.LocalObservationBus;
import org.modeshape.graph.observe.ObservationBus;
import org.modeshape.graph.property.Path;
import org.modeshape.repository.service.ServiceAdministrator;

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
    private Problems problems;
    private ObservationBus bus;
    @Mock
    private RepositoryLibrary sources;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(ModeShapeLexicon.Namespace.PREFIX, ModeShapeLexicon.Namespace.URI);
        configSourceName = "configSource";
        configWorkspaceName = null;
        configRepositorySource = new InMemoryRepositorySource();
        configRepositorySource.setName(configSourceName);
        configRepositorySource.setDefaultWorkspaceName("default");
        configRepository = Graph.create(configRepositorySource, context);
        RepositoryConnection configRepositoryConnection = configRepositorySource.getConnection();
        when(sources.createConnection(configSourceName)).thenReturn(configRepositoryConnection);
        root = context.getValueFactories().getPathFactory().createRootPath();
        problems = new SimpleProblems();
        bus = new LocalObservationBus();
        service = new RepositoryService(configRepositorySource, configWorkspaceName, root, context, bus, problems);
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

    @Test
    public void shouldStartUpUsingConfigurationRepositoryThatContainsNoSources() throws Exception {
        // Set up the configuration repository to contain NO sources ...
        configRepository.create("/mode:sources").and();

        // Now, start up the service ...
        service.getAdministrator().start();

        // and verify that the sources were never added to the manager...
        verifyNoMoreInteractions(sources);
    }

    @Test
    public void shouldStartUpAndCreateRepositoryUsingConfigurationRepositoryThatContainsNoSources() {
        // Set up the configuration repository ...
        configRepository.useWorkspace("default");
        configRepository.create("/mode:sources").and();
        configRepository.create("/mode:sources/source A").and();

        final String className = InMemoryRepositorySource.class.getName();
        configRepository.set(ModeShapeLexicon.CLASSNAME).on("/mode:sources/source A").to(className);
        configRepository.set(ModeShapeLexicon.CLASSPATH).on("/mode:sources/source A").to("");
        configRepository.set("retryLimit").on("/mode:sources/source A").to(3);

        String fedReposPath = "/mode:repositories/fed repos/";
        configRepository.create("/mode:repositories").and();
        configRepository.create("/mode:repositories/fed repos").and();
        configRepository.create("/mode:repositories/fed repos/mode:regions").and();
        configRepository.create("/mode:repositories/fed repos/mode:regions/source A").and();
        configRepository.create("/mode:repositories/fed repos/mode:regions/source B").and();
        configRepository.create("/mode:repositories/fed repos/mode:regions/source C").and();
        configRepository.create("/mode:repositories/fed repos/mode:regions/source D").and();
        configRepository.set(ModeShapeLexicon.TIME_TO_EXPIRE).on(fedReposPath).to(20000);
        configRepository.set(ModeShapeLexicon.PROJECTION_RULES).on(fedReposPath + "mode:regions/source A").to("/a/b/c => /sx/sy");
        configRepository.set(ModeShapeLexicon.PROJECTION_RULES).on(fedReposPath + "mode:regions/source B").to("/ => /");
        configRepository.set(ModeShapeLexicon.PROJECTION_RULES).on(fedReposPath + "mode:regions/source C").to("/d/e/f => /");
        configRepository.set(ModeShapeLexicon.PROJECTION_RULES).on(fedReposPath + "mode:regions/source D").to("/ => /x/y/z");

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
        Path configPath = context.getValueFactories().getPathFactory().create("/mode:system");
        service = new RepositoryService(configRepositorySource, configWorkspaceName, configPath, context, bus, problems);

        // Set up the configuration repository ...
        configRepository.useWorkspace("default");
        configRepository.create("/mode:system").and();
        configRepository.create("/mode:system/mode:sources").and();
        configRepository.create("/mode:system/mode:sources/source A").and();

        final String className = FakeRepositorySource.class.getName();
        configRepository.set(ModeShapeLexicon.CLASSNAME).on("/mode:system/mode:sources/source A").to(className);
        configRepository.set(ModeShapeLexicon.CLASSPATH).on("/mode:system/mode:sources/source A").to("");
        configRepository.set("retryLimit").on("/mode:system/mode:sources/source A").to(3);
        configRepository.set("intParam").on("/mode:system/mode:sources/source A").to("3");
        configRepository.set("shortParam").on("/mode:system/mode:sources/source A").to("32");
        configRepository.set("booleanParam").on("/mode:system/mode:sources/source A").to("true");
        configRepository.set("stringParam").on("/mode:system/mode:sources/source A").to("string value");
        configRepository.set("intArrayParam").on("/mode:system/mode:sources/source A").to("3");
        configRepository.set("booleanArrayParam").on("/mode:system/mode:sources/source A").to("true");
        configRepository.set("longObjectArrayParam").on("/mode:system/mode:sources/source A").to("987654321");
        configRepository.set("booleanObjectArrayParam").on("/mode:system/mode:sources/source A").to("true");
        configRepository.set("stringArrayParam").on("/mode:system/mode:sources/source A").to("string value");

        // Now, start up the service ...
        service.getAdministrator().start();

        // Get the source, which should be configured ...
        RepositorySource repositorySourceA = service.getRepositoryLibrary().getSource("source A");
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
