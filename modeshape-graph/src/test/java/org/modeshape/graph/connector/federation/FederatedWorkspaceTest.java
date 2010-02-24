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
package org.modeshape.graph.connector.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.federation.Projection.Rule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

/**
 * 
 */
public class FederatedWorkspaceTest {

    private FederatedWorkspace workspace;
    private ExecutionContext context;
    private CachePolicy cachePolicy;
    private String sourceName;
    private String workspaceName;
    private List<Projection> projections;
    @Mock
    private RepositoryContext repositoryContext;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        sourceName = "federated";
        workspaceName = "my workspace";
        context = new ExecutionContext();
        when(repositoryContext.getExecutionContext()).thenReturn(context);
        projections = new ArrayList<Projection>();
        projections.add(new Projection("source1", "workspace1", false, rule("/a => /a1")));
        projections.add(new Projection("source2", "workspace2", false, rule("/a => /a2", "/b => /b")));
        projections.add(new Projection("source2", "workspace4", false, rule("/x => /y")));
        projections.add(new Projection("source3", "workspace3", false, rule("/c/d/e => /c1")));
        workspace = new FederatedWorkspace(repositoryContext, sourceName, workspaceName, projections, cachePolicy);
    }

    protected Rule[] rule( String... rule ) {
        Rule[] rules = new Rule[rule.length];
        for (int i = 0; i != rule.length; ++i) {
            rules[i] = Projection.fromString(rule[i], context);
        }
        return rules;
    }

    @Test
    public void shouldHaveSameRepositoryContextPassedIntoConstructor() {
        assertThat(workspace.getRepositoryContext(), is(sameInstance(repositoryContext)));
    }

    @Test
    public void shouldHaveSameSourceNamePassedIntoConstructor() {
        assertThat(workspace.getSourceName(), is(sameInstance(sourceName)));
    }

    @Test
    public void shouldHaveSameWorkspaceNamePassedIntoConstructor() {
        assertThat(workspace.getName(), is(sameInstance(workspaceName)));
    }

    @Test
    public void shouldHaveSameCachePolicyPassedIntoContructor() {
        assertThat(workspace.getCachePolicy(), is(sameInstance(cachePolicy)));
    }

    @Test
    public void shouldHaveSameProjectionsPassedIntoConstructor() {
        assertThat(workspace.getProjections(), is(projections));
    }

    @Test
    public void shouldHaveCorrectMappingOfProjectionsBySource() {
        assertThat(workspace.getProjectionsBySourceName().get("source1"), is(projections.subList(0, 1)));
        assertThat(workspace.getProjectionsBySourceName().get("source2"), is(projections.subList(1, 3)));
        assertThat(workspace.getProjectionsBySourceName().get("source3"), is(projections.subList(3, 4)));
    }

    @Test
    public void shouldHaveProblemsContainer() {
        assertThat(workspace.getProblems(), is(notNullValue()));
    }

    @Test
    public void shouldCorrectlyDetermineContainmentBySourceAndWorkspaceName() {
        assertThat(workspace.contains("source1", "workspace1"), is(true));
        assertThat(workspace.contains("source2", "workspace2"), is(true));
        assertThat(workspace.contains("source2", "workspace4"), is(true));
        assertThat(workspace.contains("source3", "workspace3"), is(true));
    }

    @Test
    public void shouldCorrectlyDetermineNonContainmentOfCaseSensitiveSourceNotInProjections() {
        assertThat(workspace.contains("source x", "workspace3"), is(false));
        assertThat(workspace.contains("Source1", "workspace3"), is(false));
        assertThat(workspace.contains("", "workspace3"), is(false));
    }

    @Test
    public void shouldCorrectlyDetermineNonContainmentOfCaseSensitiveWorkspaceNotInProjections() {
        assertThat(workspace.contains("source1", "Workspace1"), is(false));
        assertThat(workspace.contains("source1", "workspace 1"), is(false));
        assertThat(workspace.contains("source1", "no workspace"), is(false));
    }

    @Test
    public void shouldConsiderWorkspacesEqualIfSameSourceNameAndSameWorkspaceNameAndSameProjections() {
        RepositoryContext otherReposContext = mock(RepositoryContext.class);
        when(otherReposContext.getExecutionContext()).thenReturn(context);
        CachePolicy otherPolicy = mock(CachePolicy.class);
        FederatedWorkspace other = new FederatedWorkspace(otherReposContext, sourceName, workspaceName, projections, otherPolicy);
        assertThat(workspace.equals(other), is(true));
    }

    @Test
    public void shouldConsiderWorkspacesNotEqualIfDifferentSourceName() {
        RepositoryContext otherReposContext = mock(RepositoryContext.class);
        when(otherReposContext.getExecutionContext()).thenReturn(context);
        CachePolicy otherPolicy = mock(CachePolicy.class);
        FederatedWorkspace other = new FederatedWorkspace(otherReposContext, "diff", workspaceName, projections, otherPolicy);
        assertThat(workspace.equals(other), is(false));
    }

    @Test
    public void shouldConsiderWorkspacesNotEqualIfDifferentWorkspaceName() {
        RepositoryContext otherReposContext = mock(RepositoryContext.class);
        when(otherReposContext.getExecutionContext()).thenReturn(context);
        CachePolicy otherPolicy = mock(CachePolicy.class);
        FederatedWorkspace other = new FederatedWorkspace(otherReposContext, sourceName, "diff", projections, otherPolicy);
        assertThat(workspace.equals(other), is(false));
    }

    @Test
    public void shouldConsiderWorkspacesNotEqualIfDifferentProjections() {
        RepositoryContext otherReposContext = mock(RepositoryContext.class);
        when(otherReposContext.getExecutionContext()).thenReturn(context);
        CachePolicy otherPolicy = mock(CachePolicy.class);
        FederatedWorkspace other = new FederatedWorkspace(otherReposContext, sourceName, workspaceName,
                                                          projections.subList(0, 3), otherPolicy);
        assertThat(workspace.equals(other), is(false));
    }

    @Test
    public void shouldDetermineProjection() {

    }

}
