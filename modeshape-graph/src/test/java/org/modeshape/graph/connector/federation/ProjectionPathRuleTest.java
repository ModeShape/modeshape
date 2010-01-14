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
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ProjectionPathRuleTest {

    private ExecutionContext context;
    private Projection.PathRule rule;
    private PathFactory pathFactory;
    private Path repositoryPath;
    private Path sourcePath;
    private Path[] validExceptions;
    private NamespaceRegistry registry;
    private TextEncoder encoder;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        pathFactory = context.getValueFactories().getPathFactory();
        registry = context.getNamespaceRegistry();
        encoder = new UrlEncoder();
        repositoryPath = pathFactory.create("/a/b/c");
        sourcePath = pathFactory.create("/x/y");
        validExceptions = new Path[] {pathFactory.create("e/f"), pathFactory.create("e/g")};
        rule = new Projection.PathRule(repositoryPath, sourcePath, validExceptions);
    }

    @Test
    public void shouldCreateInstanceWithValidRepositoryPathAndValidSourcePathAndNoExceptions() {
        rule = new Projection.PathRule(repositoryPath, sourcePath);
        assertThat(rule.getPathInRepository(), is(sameInstance(repositoryPath)));
        assertThat(rule.getPathInSource(), is(sameInstance(sourcePath)));
        assertThat(rule.hasExceptionsToRule(), is(false));
    }

    @Test
    public void shouldCreateInstanceWithValidRepositoryPathAndValidSourcePathAndValidExceptions() {
        rule = new Projection.PathRule(repositoryPath, sourcePath, validExceptions);
        assertThat(rule.getPathInRepository(), is(sameInstance(repositoryPath)));
        assertThat(rule.getPathInSource(), is(sameInstance(sourcePath)));
        assertThat(rule.hasExceptionsToRule(), is(true));
        assertThat(rule.getExceptionsToRule(), hasItems(validExceptions));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithNullRepositoryPathAndValidSourcePathAndNoExceptions() {
        repositoryPath = null;
        new Projection.PathRule(repositoryPath, sourcePath);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithValidRepositoryPathAndNullSourcePathAndNoExceptions() {
        sourcePath = null;
        new Projection.PathRule(repositoryPath, sourcePath);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateInstanceWithValidRepositoryPathAndValidSourcePathAndAbsoluteExceptions() {
        Path relativePath = validExceptions[0];
        Path absolutePath = pathFactory.create("/j/k/l/m");
        new Projection.PathRule(repositoryPath, sourcePath, relativePath, absolutePath);
    }

    @Test
    public void shouldIncludeRepositoryPathsAtPathInRepository() {
        assertThat(rule.includes(sourcePath), is(true));
    }

    @Test
    public void shouldIncludeRepositoryPathsBelowPathInRepositoryThatAreNotExcluded() {
        assertThat(rule.includes(pathFactory.create(sourcePath, "m")), is(true));
        assertThat(rule.includes(pathFactory.create(sourcePath, "m/n")), is(true));
        assertThat(rule.includes(pathFactory.create(sourcePath, "o/p")), is(true));
        assertThat(rule.includes(pathFactory.create(sourcePath, "e/e")), is(true));
        assertThat(rule.includes(pathFactory.create(sourcePath, "e")), is(true));
    }

    @Test
    public void shouldNotIncludeRepositoryPathsBelowPathInRepositoryThatAreExcluded() {
        assertThat(rule.includes(pathFactory.create(sourcePath, "e/f")), is(false));
        assertThat(rule.includes(pathFactory.create(sourcePath, "e/g")), is(false));
        assertThat(rule.includes(pathFactory.create(sourcePath, "e/f/g")), is(false));
        assertThat(rule.includes(pathFactory.create(sourcePath, "e/g/h")), is(false));
    }

    @Test
    public void shouldNotIncludeRepositoryPathsNotBelowPathInRepository() {
        assertThat(rule.includes(pathFactory.create("/m/n")), is(false));
        assertThat(rule.includes(pathFactory.create("/x/y[3]")), is(false));
    }

    @Test
    public void shouldProjectRepositoryPathIntoSourcePath() {
        assertThat(rule.projectPathInRepositoryToPathInSource(repositoryPath, pathFactory), is(sourcePath));
    }

    @Test
    public void shouldProjectPathBelowRepositoryPathIntoPathBelowSourcePath() {
        Path pathInRepository = pathFactory.create(repositoryPath, "m/n");
        Path pathInSource = pathFactory.create(sourcePath, "m/n");
        assertThat(rule.projectPathInRepositoryToPathInSource(pathInRepository, pathFactory), is(pathInSource));

        pathInRepository = pathFactory.create(repositoryPath, "m");
        pathInSource = pathFactory.create(sourcePath, "m");
        assertThat(rule.projectPathInRepositoryToPathInSource(pathInRepository, pathFactory), is(pathInSource));

        pathInRepository = pathFactory.create(repositoryPath, "m/n[3]");
        pathInSource = pathFactory.create(sourcePath, "m/n[3]");
        assertThat(rule.projectPathInRepositoryToPathInSource(pathInRepository, pathFactory), is(pathInSource));
    }

    @Test
    public void shouldProjectSourcePathIntoRepositoryPath() {
        assertThat(rule.projectPathInSourceToPathInRepository(sourcePath, pathFactory), is(repositoryPath));
    }

    @Test
    public void shouldProjectPathBelowSourcePathIntoPathBelowRepositoryPath() {
        Path pathInRepository = pathFactory.create(repositoryPath, "m/n");
        Path pathInSource = pathFactory.create(sourcePath, "m/n");
        assertThat(rule.projectPathInSourceToPathInRepository(pathInSource, pathFactory), is(pathInRepository));

        pathInRepository = pathFactory.create(repositoryPath, "m");
        pathInSource = pathFactory.create(sourcePath, "m");
        assertThat(rule.projectPathInSourceToPathInRepository(pathInSource, pathFactory), is(pathInRepository));

        pathInRepository = pathFactory.create(repositoryPath, "m/n[3]");
        pathInSource = pathFactory.create(sourcePath, "m/n[3]");
        assertThat(rule.projectPathInSourceToPathInRepository(pathInSource, pathFactory), is(pathInRepository));
    }

    @Test
    public void shouldGetPathsInRepositoryGivenPathsInSourceAtOrBelowSourcePathIfNotExcluded() {
        assertThat(rule.getPathInRepository(sourcePath, pathFactory), is(repositoryPath));
        assertThatGetPathInRepositoryReturnsCorrectPathInSource("");
        assertThatGetPathInRepositoryReturnsCorrectPathInSource("m/n");
        assertThatGetPathInRepositoryReturnsCorrectPathInSource("m[1]");
        assertThatGetPathInRepositoryReturnsCorrectPathInSource("m[1]/n/o/p");
    }

    protected void assertThatGetPathInRepositoryReturnsCorrectPathInSource( String subpath ) {
        assertThat(rule.getPathInRepository(pathFactory.create(sourcePath, subpath), pathFactory),
                   is(pathFactory.create(repositoryPath, subpath)));
    }

    @Test
    public void shouldGetNullPathInRepositoryGivenPathsInSourceAtOrBelowSourcePathIfExcluded() {
        assertThat(rule.getPathInRepository(pathFactory.create(sourcePath, "e/f"), pathFactory), is(nullValue()));
        assertThat(rule.getPathInRepository(pathFactory.create(sourcePath, "e/g"), pathFactory), is(nullValue()));
        assertThat(rule.getPathInRepository(pathFactory.create(sourcePath, "e/f/h"), pathFactory), is(nullValue()));
        assertThat(rule.getPathInRepository(pathFactory.create(sourcePath, "e/g/h"), pathFactory), is(nullValue()));
    }

    @Test
    public void shouldGetNullPathInRepositoryGivenPathsInRepositoryNotAtOrBelowSourcePath() {
        assertThat(rule.getPathInRepository(pathFactory.create("/m/n"), pathFactory), is(nullValue()));
    }

    @Test
    public void shouldGetPathsInSourceGivenPathsInRepositoryAtOrBelowRepositoryPathIfNotExcluded() {
        assertThat(rule.getPathInSource(repositoryPath, pathFactory), is(sourcePath));
        assertThatGetPathInSourceReturnsCorrectPathInRepository("");
        assertThatGetPathInSourceReturnsCorrectPathInRepository("m/n");
        assertThatGetPathInSourceReturnsCorrectPathInRepository("m[1]");
        assertThatGetPathInSourceReturnsCorrectPathInRepository("m[1]/n/o/p");

    }

    protected void assertThatGetPathInSourceReturnsCorrectPathInRepository( String subpath ) {
        assertThat(rule.getPathInSource(pathFactory.create(repositoryPath, subpath), pathFactory),
                   is(pathFactory.create(sourcePath, subpath)));
    }

    @Test
    public void shouldGetNullPathInSourceGivenPathsInRepositoryAtOrBelowRepositoryPathIfExcluded() {
        assertThat(rule.getPathInSource(pathFactory.create(repositoryPath, "e/f"), pathFactory), is(nullValue()));
        assertThat(rule.getPathInSource(pathFactory.create(repositoryPath, "e/g"), pathFactory), is(nullValue()));
        assertThat(rule.getPathInSource(pathFactory.create(repositoryPath, "e/f/h"), pathFactory), is(nullValue()));
        assertThat(rule.getPathInSource(pathFactory.create(repositoryPath, "e/g/h"), pathFactory), is(nullValue()));
    }

    @Test
    public void shouldGetNullPathInSourceGivenPathsInRepositoryNotAtOrBelowRepositoryPath() {
        assertThat(rule.getPathInSource(pathFactory.create("/m/n"), pathFactory), is(nullValue()));
    }

    @Test
    public void shouldConvertToString() {
        assertThat(rule.getString(registry, encoder), is("/a/b/c => /x/y $ e/f $ e/g"));

        repositoryPath = pathFactory.create("/a/b/c");
        sourcePath = pathFactory.create("/");
        rule = new Projection.PathRule(repositoryPath, sourcePath, validExceptions);
        assertThat(rule.getString(registry, encoder), is("/a/b/c => / $ e/f $ e/g"));

        repositoryPath = pathFactory.create("/");
        sourcePath = pathFactory.create("/");
        rule = new Projection.PathRule(repositoryPath, sourcePath, validExceptions);
        assertThat(rule.getString(registry, encoder), is("/ => / $ e/f $ e/g"));
    }

    @Test
    public void shouldHaveToString() {
        assertThat(rule.toString(), is("/{}a/{}b/{}c => /{}x/{}y $ {}e/{}f $ {}e/{}g"));

        repositoryPath = pathFactory.create("/a/b/c");
        sourcePath = pathFactory.create("/");
        rule = new Projection.PathRule(repositoryPath, sourcePath, validExceptions);
        assertThat(rule.toString(), is("/{}a/{}b/{}c => / $ {}e/{}f $ {}e/{}g"));
    }

}
