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
package org.jboss.dna.connector.federation.executor;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.jboss.dna.common.collection.IsIteratorContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.connector.federation.Projection;
import org.jboss.dna.connector.federation.ProjectionParser;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.graph.BasicExecutionContext;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.cache.BasicCachePolicy;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositoryConnectionFactory;
import org.jboss.dna.graph.connectors.SimpleRepository;
import org.jboss.dna.graph.connectors.SimpleRepositorySource;
import org.jboss.dna.graph.properties.DateTime;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FederatingCommandExecutorTest {

    private FederatingCommandExecutor executor;
    private ExecutionContext context;
    private PathFactory pathFactory;
    private String sourceName;
    private Projection cacheProjection;
    private CachePolicy cachePolicy;
    private List<Projection> sourceProjections;
    private Projection.Rule[] cacheProjectionRules = new Projection.Rule[] {};
    private SimpleRepositorySource cacheSource;
    private SimpleRepositorySource source1;
    private SimpleRepositorySource source2;
    private SimpleRepositorySource source3;
    @Mock
    private RepositoryConnectionFactory connectionFactory;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new BasicExecutionContext();
        pathFactory = context.getValueFactories().getPathFactory();
        sourceName = "Federated Source";
        cachePolicy = new BasicCachePolicy(10L, TimeUnit.SECONDS);
        cacheSource = new SimpleRepositorySource();
        cacheSource.setName("Cache");
        cacheSource.setRepositoryName("Cache Repository");
        ProjectionParser ruleParser = ProjectionParser.getInstance();
        cacheProjectionRules = ruleParser.rulesFromStrings(context, "/ => /cache/repo/A");
        cacheProjection = new Projection(cacheSource.getName(), cacheProjectionRules);
        source1 = new SimpleRepositorySource();
        source2 = new SimpleRepositorySource();
        source3 = new SimpleRepositorySource();
        source1.setName("Source 1");
        source2.setName("Source 2");
        source3.setName("Source 3");
        source1.setRepositoryName("Repository 1");
        source2.setRepositoryName("Repository 2");
        source3.setRepositoryName("Repository 3");
        // Set up the cache policies ...
        source1.setDefaultCachePolicy(new BasicCachePolicy(100, TimeUnit.SECONDS));
        source2.setDefaultCachePolicy(new BasicCachePolicy(200, TimeUnit.SECONDS));
        source3.setDefaultCachePolicy(new BasicCachePolicy(300, TimeUnit.SECONDS));
        sourceProjections = new ArrayList<Projection>();
        // Source 1 projects from '/source/one/a' into repository '/a'
        // and from '/source/one/b' into repository '/b'
        sourceProjections.add(new Projection(source1.getName(), ruleParser.rulesFromStrings(context,
                                                                                            "/a => /source/one/a",
                                                                                            "/b => /source/one/b")));
        // Source 2 projects from '/source/two/a' into repository '/a'
        sourceProjections.add(new Projection(source2.getName(), ruleParser.rulesFromStrings(context, "/a => /source/two/a")));
        // Source 3 projects everything into repository at root
        sourceProjections.add(new Projection(source3.getName(), ruleParser.rulesFromStrings(context, "/ => /")));
        executor = new FederatingCommandExecutor(context, sourceName, cacheProjection, cachePolicy, sourceProjections,
                                                 connectionFactory);
        // stub(connectionFactory.createConnection(source1.getName())).toReturn(source1.getConnection());
        doReturn(source1.getConnection()).when(connectionFactory).createConnection(source1.getName());
        doReturn(source2.getConnection()).when(connectionFactory).createConnection(source2.getName());
        doReturn(source3.getConnection()).when(connectionFactory).createConnection(source3.getName());
        doReturn(cacheSource.getConnection()).when(connectionFactory).createConnection(cacheSource.getName());
    }

    @After
    public void afterEach() throws Exception {
        SimpleRepository.shutdownAll();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenExecutionContextIsNull() {
        context = null;
        executor = new FederatingCommandExecutor(context, sourceName, cacheProjection, cachePolicy, sourceProjections,
                                                 connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenSourceNameIsNull() {
        sourceName = null;
        executor = new FederatingCommandExecutor(context, sourceName, cacheProjection, cachePolicy, sourceProjections,
                                                 connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenSourceNameIsEmpty() {
        sourceName = "";
        executor = new FederatingCommandExecutor(context, sourceName, cacheProjection, cachePolicy, sourceProjections,
                                                 connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenSourceNameIsBlank() {
        sourceName = "   ";
        executor = new FederatingCommandExecutor(context, sourceName, cacheProjection, cachePolicy, sourceProjections,
                                                 connectionFactory);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailWhenConnectionFactoryIsNull() {
        connectionFactory = null;
        executor = new FederatingCommandExecutor(context, sourceName, cacheProjection, cachePolicy, sourceProjections,
                                                 connectionFactory);
    }

    @Test
    public void shouldNotFailWhenCacheProjectionIsNullAndCachePolicyIsNull() {
        cachePolicy = null;
        cacheProjection = null;
        executor = new FederatingCommandExecutor(context, sourceName, cacheProjection, cachePolicy, sourceProjections,
                                                 connectionFactory);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCacheProjectionIsNullAndCachePolicyIsNotNull() {
        cachePolicy = null;
        executor = new FederatingCommandExecutor(context, sourceName, cacheProjection, cachePolicy, sourceProjections,
                                                 connectionFactory);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCacheProjectionIsProvidedToConstructorButCachePolicyIsNot() {
        assertThat(cacheProjection, is(nullValue()));
        cachePolicy = null;
        executor = new FederatingCommandExecutor(context, sourceName, cacheProjection, cachePolicy, sourceProjections,
                                                 connectionFactory);
    }

    @Test
    public void shouldHaveCurrentTimeInUtc() {
        DateTime currentTimeInUtc = executor.getCurrentTimeInUtc();
        assertThat(currentTimeInUtc, is(notNullValue()));
        assertThat(currentTimeInUtc.toUtcTimeZone(), is(currentTimeInUtc));
    }

    @Test
    public void shouldReturnSameExecutionContextSuppliedToConstructor() {
        assertThat(executor.getExecutionContext(), is(sameInstance(context)));
    }

    @Test
    public void shouldObtainCacheConnectionFromConnectionFactoryThenHoldOntoReference() throws Exception {
        RepositoryConnection connection = mock(RepositoryConnection.class);
        stub(connectionFactory.createConnection(cacheSource.getName())).toReturn(connection);
        assertThat(executor.getConnectionToCache(), is(sameInstance(connection)));
        verify(connectionFactory, times(1)).createConnection(cacheSource.getName());
        // Call it again, and should not ask the factory ...
        assertThat(executor.getConnectionToCache(), is(sameInstance(connection)));
        verifyNoMoreInteractions(connectionFactory);
    }

    @Test
    public void shouldObtainRepositoryConnectionFromConnectionFactoryThenHoldOntoReference() throws Exception {
        String sourceName = "Some source";
        Projection projection = mock(Projection.class);
        stub(projection.getSourceName()).toReturn(sourceName);
        RepositoryConnection connection = mock(RepositoryConnection.class);
        stub(connectionFactory.createConnection(sourceName)).toReturn(connection);

        assertThat(executor.getConnection(projection), is(sameInstance(connection)));
        verify(connectionFactory, times(1)).createConnection(sourceName);
        // Call it again, and should not ask the factory ...
        assertThat(executor.getConnection(projection), is(sameInstance(connection)));
        verifyNoMoreInteractions(connectionFactory);
    }

    @Test
    public void shouldCloseHavingNotOpenedConnections() throws Exception {
        assertThat(executor.getOpenConnections().isEmpty(), is(true));
        executor.close();
        assertThat(executor.getOpenConnections().isEmpty(), is(true));
        verifyNoMoreInteractions(connectionFactory);
    }

    @Test
    public void shouldCloseAllOpenConnectionsWhenClosingExecutor() throws Exception {
        // Load the connections
        assertThat(executor.getConnectionToCache(), is(notNullValue()));
        for (Projection projection : executor.getSourceProjections()) {
            assertThat(executor.getConnection(projection), is(notNullValue()));
        }
        verify(connectionFactory).createConnection(cacheSource.getName());
        verify(connectionFactory).createConnection(source1.getName());
        verify(connectionFactory).createConnection(source2.getName());
        verify(connectionFactory).createConnection(source3.getName());
        assertThat(executor.getOpenConnections().isEmpty(), is(false));
        // Close the executor and verify all connections have been closed
        executor.close();
        assertThat(executor.getOpenConnections().isEmpty(), is(true));
        verifyNoMoreInteractions(connectionFactory);
    }

    @Test
    public void shouldLoadContributionsForRootNodeFromSources() throws Exception {
        Path path = pathFactory.createRootPath();
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(new Location(path), null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        Location childA = new Location(pathFactory.create(path, "a"));
        Location childB = new Location(pathFactory.create(path, "b"));
        assertThat(contributions.get(0).getChildren(), hasItems(childA, childB));
        assertThat(contributions.get(1).getChildren(), hasItems(childA));
        assertThat(contributions.get(2).getChildrenCount(), is(0));
    }

    protected void hasChildren( Contribution contribution,
                                String... childNames ) {
        Location location = contribution.getLocationInSource();
        Iterator<Location> iter = contribution.getChildren();
        for (String childName : childNames) {
            Path expectedChildPath = context.getValueFactories().getPathFactory().create(location.getPath(), childName);
            Location expectedChild = new Location(expectedChildPath);
            Location next = iter.next();
            if (!next.isSame(expectedChild)) {
                assertThat(next, is(expectedChild));
            }
        }
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldLoadContributionsForNonRootNodeWithOneContributionFromSources() throws Exception {
        // Set up the content of source 3
        SimpleRepository repository3 = SimpleRepository.get(source3.getRepositoryName());
        repository3.setProperty(context, "/x/y", "desc", "y escription");
        repository3.setProperty(context, "/x/y/zA", "desc", "zA description");
        repository3.setProperty(context, "/x/y/zB", "desc", "zB description");
        repository3.setProperty(context, "/x/y/zC", "desc", "zC description");

        Path path = pathFactory.create("/x/y"); // from source 3
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(new Location(path), null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0));
        hasChildren(contributions.get(1));
        hasChildren(contributions.get(2), "zA", "zB", "zC");

        path = pathFactory.create("/x"); // from source 3
        contributions.clear();
        executor.loadContributionsFromSources(new Location(path), null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0));
        hasChildren(contributions.get(1));
        hasChildren(contributions.get(2), "y");
    }

    @Test
    public void shouldLoadNonRootNodeWithTwoContributionFromSources() throws Exception {
        // Set up the content of source 1
        SimpleRepository repository1 = SimpleRepository.get(source1.getRepositoryName());
        repository1.setProperty(context, "/source/one/a", "desc", "source 1 node a escription");
        repository1.setProperty(context, "/source/one/a/nA", "desc", "source 1 node nA description");
        repository1.setProperty(context, "/source/one/a/nB", "desc", "source 1 node nB description");
        repository1.setProperty(context, "/source/one/a/nC", "desc", "source 1 node nC description");
        repository1.setProperty(context, "/source/one/b", "desc", "source 1 node b description");
        repository1.setProperty(context, "/source/one/b/pA", "desc", "source 1 node pA description");
        repository1.setProperty(context, "/source/one/b/pB", "desc", "source 1 node pB description");
        repository1.setProperty(context, "/source/one/b/pC", "desc", "source 1 node pC description");
        // Set up the content of source 2
        SimpleRepository repository2 = SimpleRepository.get(source2.getRepositoryName());
        repository2.setProperty(context, "/source/two/a", "desc", "source 2 node a escription");
        repository2.setProperty(context, "/source/two/a/qA", "desc", "source 2 node qA description");
        repository2.setProperty(context, "/source/two/a/qB", "desc", "source 2 node qB description");
        repository2.setProperty(context, "/source/two/a/qC", "desc", "source 2 node qC description");
        // Set up the content of source 3
        SimpleRepository repository3 = SimpleRepository.get(source3.getRepositoryName());
        repository3.setProperty(context, "/x/y", "desc", "y escription");
        repository3.setProperty(context, "/x/y/zA", "desc", "zA description");
        repository3.setProperty(context, "/x/y/zB", "desc", "zB description");
        repository3.setProperty(context, "/x/y/zC", "desc", "zC description");
        repository3.setProperty(context, "/b/by", "desc", "by escription");
        repository3.setProperty(context, "/b/by/bzA", "desc", "bzA description");
        repository3.setProperty(context, "/b/by/bzB", "desc", "bzB description");

        Path path = pathFactory.create("/b"); // from source 2 and source 3
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(new Location(path), null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0), "pA", "pB", "pC");
        hasChildren(contributions.get(1));
        hasChildren(contributions.get(2), "by");

        path = pathFactory.create("/b/by"); // from source 3
        contributions.clear();
        executor.loadContributionsFromSources(new Location(path), null, contributions);

        assertThat(contributions.size(), is(2)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0));
        hasChildren(contributions.get(1), "bzA", "bzB");
    }

    @Test
    public void shouldLoadNonRootNodeWithThreeContributionFromSources() throws Exception {
        // Set up the content of source 1
        SimpleRepository repository1 = SimpleRepository.get(source1.getRepositoryName());
        repository1.setProperty(context, "/source/one/a", "desc", "source 1 node a escription");
        repository1.setProperty(context, "/source/one/a/nA", "desc", "source 1 node nA description");
        repository1.setProperty(context, "/source/one/a/nB", "desc", "source 1 node nB description");
        repository1.setProperty(context, "/source/one/a/nC", "desc", "source 1 node nC description");
        repository1.setProperty(context, "/source/one/b", "desc", "source 1 node b description");
        repository1.setProperty(context, "/source/one/b/pA", "desc", "source 1 node pA description");
        repository1.setProperty(context, "/source/one/b/pB", "desc", "source 1 node pB description");
        repository1.setProperty(context, "/source/one/b/pC", "desc", "source 1 node pC description");
        // Set up the content of source 2
        SimpleRepository repository2 = SimpleRepository.get(source2.getRepositoryName());
        repository2.setProperty(context, "/source/two/a", "desc", "source 2 node a escription");
        repository2.setProperty(context, "/source/two/a/qA", "desc", "source 2 node qA description");
        repository2.setProperty(context, "/source/two/a/qB", "desc", "source 2 node qB description");
        repository2.setProperty(context, "/source/two/a/qC", "desc", "source 2 node qC description");
        // Set up the content of source 3
        SimpleRepository repository3 = SimpleRepository.get(source3.getRepositoryName());
        repository3.setProperty(context, "/x/y", "desc", "y escription");
        repository3.setProperty(context, "/x/y/zA", "desc", "zA description");
        repository3.setProperty(context, "/x/y/zB", "desc", "zB description");
        repository3.setProperty(context, "/x/y/zC", "desc", "zC description");
        repository3.setProperty(context, "/b/by", "desc", "by escription");
        repository3.setProperty(context, "/b/by/bzA", "desc", "bzA description");
        repository3.setProperty(context, "/b/by/bzB", "desc", "bzB description");
        repository3.setProperty(context, "/a/ay", "desc", "by escription");
        repository3.setProperty(context, "/a/ay/azA", "desc", "bzA description");
        repository3.setProperty(context, "/a/ay/azB", "desc", "bzB description");

        Path path = pathFactory.create("/a"); // from sources 1, 2 and 3
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(new Location(path), null, contributions);

        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));

        hasChildren(contributions.get(0), "nA", "nB", "nC");
        hasChildren(contributions.get(1), "qA", "qB", "qC");
        hasChildren(contributions.get(2), "ay");

        path = pathFactory.create("/a/ay"); // from source 3
        contributions.clear();
        executor.loadContributionsFromSources(new Location(path), null, contributions);

        assertThat(contributions.size(), is(1)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source3.getName()));
        hasChildren(contributions.get(0), "azA", "azB");
    }

    @Test
    public void shouldFailToLoadNodeFromSourcesWhenTheNodeDoesNotAppearInAnyOfTheSources() throws Exception {
        Path nonExistant = pathFactory.create("/nonExistant/Node/In/AnySource");
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(new Location(nonExistant), null, contributions);
        // All of the contributions should be empty ...
        for (Contribution contribution : contributions) {
            assertThat(contribution.isEmpty(), is(true));
        }
    }

    @Test
    public void shouldComputeCachePolicyCorrectlyUsingCurrentTimeAndSourceDefaultCachePolicy() throws Exception {
        // Set up the content of source 1
        SimpleRepository repository1 = SimpleRepository.get(source1.getRepositoryName());
        repository1.setProperty(context, "/source/one/a", "desc", "source 1 node a escription");
        repository1.setProperty(context, "/source/one/a/nA", "desc", "source 1 node nA description");
        repository1.setProperty(context, "/source/one/a/nB", "desc", "source 1 node nB description");
        repository1.setProperty(context, "/source/one/a/nC", "desc", "source 1 node nC description");
        repository1.setProperty(context, "/source/one/b", "desc", "source 1 node b description");
        repository1.setProperty(context, "/source/one/b/pA", "desc", "source 1 node pA description");
        repository1.setProperty(context, "/source/one/b/pB", "desc", "source 1 node pB description");
        repository1.setProperty(context, "/source/one/b/pC", "desc", "source 1 node pC description");
        // Set up the content of source 2
        SimpleRepository repository2 = SimpleRepository.get(source2.getRepositoryName());
        repository2.setProperty(context, "/source/two/a", "desc", "source 2 node a escription");
        repository2.setProperty(context, "/source/two/a/qA", "desc", "source 2 node qA description");
        repository2.setProperty(context, "/source/two/a/qB", "desc", "source 2 node qB description");
        repository2.setProperty(context, "/source/two/a/qC", "desc", "source 2 node qC description");
        // Set up the content of source 3
        SimpleRepository repository3 = SimpleRepository.get(source3.getRepositoryName());
        repository3.setProperty(context, "/x/y", "desc", "y escription");
        repository3.setProperty(context, "/x/y/zA", "desc", "zA description");
        repository3.setProperty(context, "/x/y/zB", "desc", "zB description");
        repository3.setProperty(context, "/x/y/zC", "desc", "zC description");
        repository3.setProperty(context, "/b/by", "desc", "by escription");
        repository3.setProperty(context, "/b/by/bzA", "desc", "bzA description");
        repository3.setProperty(context, "/b/by/bzB", "desc", "bzB description");
        repository3.setProperty(context, "/a/ay", "desc", "by escription");
        repository3.setProperty(context, "/a/ay/azA", "desc", "bzA description");
        repository3.setProperty(context, "/a/ay/azB", "desc", "bzB description");

        Path path = pathFactory.create("/a"); // from sources 1, 2 and 3
        List<Contribution> contributions = new LinkedList<Contribution>();
        executor.loadContributionsFromSources(new Location(path), null, contributions);

        // Check when the contributions expire ...
        DateTime nowInUtc = executor.getCurrentTimeInUtc();
        DateTime nowPlus10InUtc = nowInUtc.plusSeconds(10);
        DateTime nowPlus110InUtc = nowInUtc.plusSeconds(110);
        DateTime nowPlus210InUtc = nowInUtc.plusSeconds(210);
        DateTime nowPlus310InUtc = nowInUtc.plusSeconds(310);
        assertThat(contributions.size(), is(3)); // order is based upon order of projections
        assertThat(contributions.get(0).getSourceName(), is(source1.getName()));
        assertThat(contributions.get(1).getSourceName(), is(source2.getName()));
        assertThat(contributions.get(2).getSourceName(), is(source3.getName()));
        assertThat(contributions.get(0).isExpired(nowPlus10InUtc), is(false));
        assertThat(contributions.get(0).isExpired(nowPlus110InUtc), is(true));
        assertThat(contributions.get(1).isExpired(nowPlus10InUtc), is(false));
        assertThat(contributions.get(1).isExpired(nowPlus210InUtc), is(true));
        assertThat(contributions.get(2).isExpired(nowPlus10InUtc), is(false));
        assertThat(contributions.get(2).isExpired(nowPlus210InUtc), is(false));
        assertThat(contributions.get(2).isExpired(nowPlus310InUtc), is(true));
    }

    @Test
    public void shouldGetNodeUsingPath() {

    }

}
