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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.when;
import java.util.Set;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

/**
 * @author Randall Hauch
 */
public class ProjectionTest {

    private ExecutionContext context;
    private String sourceName;
    private String workspaceName;
    private Projection.Rule[] rules;
    private Projection projection;
    private PathFactory pathFactory;
    private boolean readOnly;
    @Mock
    private Projection.Rule mockRule1;
    @Mock
    private Projection.Rule mockRule2;
    @Mock
    private Projection.Rule mockRule3;

    @Before
    public void beforeEach() {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        pathFactory = context.getValueFactories().getPathFactory();
        sourceName = "Valid name";
        workspaceName = "Valid workspace";
        readOnly = false;
        rules = new Projection.Rule[] {mockRule1, mockRule2, mockRule3};
        projection = new Projection(sourceName, workspaceName, readOnly, rules);
    }

    @Test
    public void shouldCreateInstanceWithValidNameAndValidRules() {
        projection = new Projection(sourceName, workspaceName, readOnly, rules);
        assertThat(projection.getSourceName(), is(sourceName));
        assertThat(projection.getWorkspaceName(), is(workspaceName));
        assertThat(projection.getRules().size(), is(rules.length));
        assertThat(projection.getRules(), hasItems(mockRule1, mockRule2, mockRule3));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithNullNameAndValidRules() {
        sourceName = null;
        projection = new Projection(sourceName, workspaceName, readOnly, rules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithNullWorkspaceNameAndValidSourceNameAndRules() {
        sourceName = null;
        projection = new Projection(sourceName, workspaceName, readOnly, rules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithEmptyNameAndValidRules() {
        sourceName = "";
        projection = new Projection(sourceName, workspaceName, readOnly, rules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithBlankNameAndValidRules() {
        sourceName = "   \t ";
        projection = new Projection(sourceName, workspaceName, readOnly, rules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithValidNameAndNullRules() {
        rules = null;
        projection = new Projection(sourceName, workspaceName, readOnly, rules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithValidNameAndEmptyRules() {
        rules = new Projection.Rule[] {};
        projection = new Projection(sourceName, workspaceName, readOnly, rules);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithValidNameAndRulesArrayContainingAllNulls() {
        projection = new Projection(sourceName, null, readOnly, null, null);
    }

    @Test
    public void shouldCreateInstanceWithValidNameAndRulesAndShouldPruneNullRuleReferences() {
        projection = new Projection(sourceName, workspaceName, readOnly, mockRule1, null, mockRule3);
        assertThat(projection.getRules().size(), is(2));
        assertThat(projection.getRules(), hasItems(mockRule1, mockRule3));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetPathsInSourceGivenValidPathAndNullPathFactory() {
        Path pathInRepository = pathFactory.create("/a/b/c");
        projection.getPathsInSource(pathInRepository, null);
    }

    @Test
    public void shouldGetNoPathsInSourceGivenNullPathInRepository() {
        Set<Path> pathsInSource = projection.getPathsInSource(null, pathFactory);
        assertThat(pathsInSource.isEmpty(), is(true));
    }

    @Test
    public void shouldGetNoPathsInSourceGivenPathInRepositoryAndNoApplicableRules() {
        Path pathInRepository = pathFactory.create("/a/b/c");
        when(mockRule1.getPathInSource(pathInRepository, pathFactory)).thenReturn(null);
        when(mockRule2.getPathInSource(pathInRepository, pathFactory)).thenReturn(null);
        when(mockRule3.getPathInSource(pathInRepository, pathFactory)).thenReturn(null);
        Set<Path> pathsInSource = projection.getPathsInSource(pathInRepository, pathFactory);
        assertThat(pathsInSource.isEmpty(), is(true));
    }

    @Test
    public void shouldGetPathInSourceGivenPathInRepositoryAndOneApplicableRules() {
        Path pathInRepository = pathFactory.create("/a/b/c");
        Path pathInSource = pathFactory.create("/d/e/f");
        when(mockRule1.getPathInSource(pathInRepository, pathFactory)).thenReturn(pathInSource);
        when(mockRule2.getPathInSource(pathInRepository, pathFactory)).thenReturn(null);
        when(mockRule3.getPathInSource(pathInRepository, pathFactory)).thenReturn(null);
        Set<Path> pathsInSource = projection.getPathsInSource(pathInRepository, pathFactory);
        assertThat(pathsInSource, hasItems(pathInSource));
    }

    @Test
    public void shouldGetPathsInSourceGivenPathInRepositoryAndMultipleApplicableRules() {
        Path pathInRepository = pathFactory.create("/a/b/c");
        Path pathInSource1 = pathFactory.create("/d/e/f");
        Path pathInSource2 = pathFactory.create("/d/e/g");
        Path pathInSource3 = pathFactory.create("/d/e/h");
        when(mockRule1.getPathInSource(pathInRepository, pathFactory)).thenReturn(pathInSource1);
        when(mockRule2.getPathInSource(pathInRepository, pathFactory)).thenReturn(pathInSource2);
        when(mockRule3.getPathInSource(pathInRepository, pathFactory)).thenReturn(pathInSource3);
        Set<Path> pathsInSource = projection.getPathsInSource(pathInRepository, pathFactory);
        assertThat(pathsInSource, hasItems(pathInSource1, pathInSource2, pathInSource3));
    }

    @Test
    public void shouldGetPathsInSourceGivenPathInRepositoryAndMultipleApplicableRulesReturningDuplicatePathsInSource() {
        Path pathInRepository = pathFactory.create("/a/b/c");
        Path pathInSource1 = pathFactory.create("/d/e/f");
        Path pathInSource23 = pathFactory.create("/d/e/g");
        when(mockRule1.getPathInSource(pathInRepository, pathFactory)).thenReturn(pathInSource1);
        when(mockRule2.getPathInSource(pathInRepository, pathFactory)).thenReturn(pathInSource23);
        when(mockRule3.getPathInSource(pathInRepository, pathFactory)).thenReturn(pathInSource23);
        Set<Path> pathsInSource = projection.getPathsInSource(pathInRepository, pathFactory);
        assertThat(pathsInSource, hasItems(pathInSource1, pathInSource23));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetPathsInRepositoryGivenValidPathAndNullPathFactory() {
        Path pathInSource = pathFactory.create("/a/b/c");
        projection.getPathsInRepository(pathInSource, null);
    }

    @Test
    public void shouldGetNoPathsInRepositoryGivenNullPathInSource() {
        Set<Path> pathsInRepository = projection.getPathsInRepository(null, pathFactory);
        assertThat(pathsInRepository.isEmpty(), is(true));
    }

    @Test
    public void shouldGetNoPathsInRepositoryGivenPathInSourceAndNoApplicableRules() {
        Path pathInSource = pathFactory.create("/d/e/f");
        when(mockRule1.getPathInRepository(pathInSource, pathFactory)).thenReturn(null);
        when(mockRule2.getPathInRepository(pathInSource, pathFactory)).thenReturn(null);
        when(mockRule3.getPathInRepository(pathInSource, pathFactory)).thenReturn(null);
        Set<Path> pathsInRepository = projection.getPathsInRepository(pathInSource, pathFactory);
        assertThat(pathsInRepository.isEmpty(), is(true));
    }

    @Test
    public void shouldGetPathInRepositoryGivenPathInSourceAndOneApplicableRules() {
        Path pathInRepository = pathFactory.create("/a/b/c");
        Path pathInSource = pathFactory.create("/d/e/f");
        when(mockRule1.getPathInRepository(pathInSource, pathFactory)).thenReturn(pathInRepository);
        when(mockRule2.getPathInRepository(pathInSource, pathFactory)).thenReturn(null);
        when(mockRule3.getPathInRepository(pathInSource, pathFactory)).thenReturn(null);
        Set<Path> pathsInRepository = projection.getPathsInRepository(pathInSource, pathFactory);
        assertThat(pathsInRepository, hasItems(pathInRepository));
    }

    @Test
    public void shouldGetPathsInRepositoryGivenPathInSourceAndMultipleApplicableRules() {
        Path pathInSource = pathFactory.create("/a/b/c");
        Path pathInRepository1 = pathFactory.create("/d/e/f");
        Path pathInRepository2 = pathFactory.create("/d/e/g");
        Path pathInRepository3 = pathFactory.create("/d/e/h");
        when(mockRule1.getPathInRepository(pathInSource, pathFactory)).thenReturn(pathInRepository1);
        when(mockRule2.getPathInRepository(pathInSource, pathFactory)).thenReturn(pathInRepository2);
        when(mockRule3.getPathInRepository(pathInSource, pathFactory)).thenReturn(pathInRepository3);
        Set<Path> pathsInRepository = projection.getPathsInRepository(pathInSource, pathFactory);
        assertThat(pathsInRepository, hasItems(pathInRepository1, pathInRepository2, pathInRepository3));
    }

    @Test
    public void shouldGetPathsInRepositoryGivenPathInSourceAndMultipleApplicableRulesReturningDuplicatePathsInRepository() {
        Path pathInSource = pathFactory.create("/a/b/c");
        Path pathInRepository1 = pathFactory.create("/d/e/f");
        Path pathInRepository23 = pathFactory.create("/d/e/g");
        when(mockRule1.getPathInRepository(pathInSource, pathFactory)).thenReturn(pathInRepository1);
        when(mockRule2.getPathInRepository(pathInSource, pathFactory)).thenReturn(pathInRepository23);
        when(mockRule3.getPathInRepository(pathInSource, pathFactory)).thenReturn(pathInRepository23);
        Set<Path> pathsInRepository = projection.getPathsInRepository(pathInSource, pathFactory);
        assertThat(pathsInRepository, hasItems(pathInRepository1, pathInRepository23));
    }

    @Test
    public void shouldParsePathRuleFromDefinitionWithNonRootRepositoryPathAndNonRootSourcePath() {
        Projection.Rule rule = Projection.parsePathRule("/a => /b", context);
        assertThat(rule, is(instanceOf(Projection.PathRule.class)));
        Projection.PathRule pathRule = (Projection.PathRule)rule;
        assertThat(pathRule.getPathInRepository(), is(pathFactory.create("/a")));
        assertThat(pathRule.getPathInSource(), is(pathFactory.create("/b")));
    }

    @Test
    public void shouldParsePathRuleFromDefinitionWithRootRepositoryPathAndNonRootSourcePath() {
        Projection.Rule rule = Projection.parsePathRule("/ => /b", context);
        assertThat(rule, is(instanceOf(Projection.PathRule.class)));
        Projection.PathRule pathRule = (Projection.PathRule)rule;
        assertThat(pathRule.getPathInRepository(), is(pathFactory.createRootPath()));
        assertThat(pathRule.getPathInSource(), is(pathFactory.create("/b")));
    }

    @Test
    public void shouldParsePathRuleFromDefinitionWithNonRootRepositoryPathAndRootSourcePath() {
        Projection.Rule rule = Projection.parsePathRule("/a => /", context);
        assertThat(rule, is(instanceOf(Projection.PathRule.class)));
        Projection.PathRule pathRule = (Projection.PathRule)rule;
        assertThat(pathRule.getPathInRepository(), is(pathFactory.create("/a")));
        assertThat(pathRule.getPathInSource(), is(pathFactory.createRootPath()));
    }

    @Test
    public void shouldParsePathRuleFromDefinitionWithRootRepositoryPathAndRootSourcePath() {
        Projection.Rule rule = Projection.parsePathRule("/ => /", context);
        assertThat(rule, is(instanceOf(Projection.PathRule.class)));
        Projection.PathRule pathRule = (Projection.PathRule)rule;
        assertThat(pathRule.getPathInRepository(), is(pathFactory.createRootPath()));
        assertThat(pathRule.getPathInSource(), is(pathFactory.createRootPath()));
    }

    @Test
    public void shouldNotParsePathRuleFromDefinitionWithRootRepositoryPathAndNoSourcePath() {
        assertThat(Projection.parsePathRule("/", context), is(nullValue()));
    }

    @Test
    public void shouldNotParsePathRuleFromDefinitionWithNonRootRepositoryPathAndNoSourcePath() {
        assertThat(Projection.parsePathRule("a/", context), is(nullValue()));
    }
}
