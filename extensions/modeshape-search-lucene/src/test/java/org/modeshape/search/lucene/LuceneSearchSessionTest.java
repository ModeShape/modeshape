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
package org.modeshape.search.lucene;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.search.AbstractSearchEngine.Workspaces;

public class LuceneSearchSessionTest {

    private ExecutionContext context;
    private LuceneSearchProcessor processor;
    private LuceneSearchSession search;
    private Observer observer;

    @SuppressWarnings( "unchecked" )
    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        DateTime now = context.getValueFactories().getDateFactory().create();
        Workspaces<LuceneSearchWorkspace> workspaces = mock(Workspaces.class);
        LuceneSearchWorkspace workspace = mock(LuceneSearchWorkspace.class);
        processor = new LuceneSearchProcessor("source", context, workspaces, observer, now, true);
        search = new LuceneSearchSession(workspace, processor);
    }

    @Test
    public void shouldCreateCorrectLikeExpressionsForAbolutePathsWithAnyWildcard() {
        assertPathLike("/", "/");
        assertPathLike("/a", "/a[1]");
        assertPathLike("/a", "/a[1]");
        assertPathLike("/a/b", "/a[1]/b[1]");
        assertPathLike("/a[2]", "/a[2]");
        assertPathLike("/a[2]/b", "/a[2]/b[1]");
        assertPathLike("/a[2]/%/b", "/a[2]/%/b[1]");
        assertPathLike("/a[2]/%/b%", "/a[2]/%/b%[1]");
        assertPathLike("/a[2]/%/b%[%]", "/a[2]/%/b%[%]");
        assertPathLike("/a[2]/%/b%[%]%", "/a[2]/%/b%[%]%");
        assertPathLike("/a[2]/%/b%[3]%", "/a[2]/%/b%[3]%");
        assertPathLike("/a[2]/%%%/b%[3]%%%", "/a[2]/%/b%[3]%");
        assertPathLike("/a[2]/%%%/b%[%%%]%%%", "/a[2]/%/b%[%]%");
    }

    @Test
    public void shouldCreateCorrectLikeExpressionsForAbolutePathsWithSingleCharacterWildcard() {
        assertPathLike("/", "/");
        assertPathLike("/a", "/a[1]");
        assertPathLike("/a", "/a[1]");
        assertPathLike("/a_", "/a_[1]");
        assertPathLike("/a/b", "/a[1]/b[1]");
        assertPathLike("/a[2]", "/a[2]");
        assertPathLike("/a[2]/b", "/a[2]/b[1]");
        assertPathLike("/a[2]/_/b", "/a[2]/_/b[1]");
        assertPathLike("/a[2]/_/b%", "/a[2]/_/b%[1]");
        assertPathLike("/a[2]/_/b%[%]", "/a[2]/_/b%[%]");
        assertPathLike("/a[2]/_/b%[%]%", "/a[2]/_/b%[%]%");
        assertPathLike("/a[2]/_/b%[3]%", "/a[2]/_/b%[3]%");
        assertPathLike("/a[2]/_/b_", "/a[2]/_/b_[1]");
        assertPathLike("/a[2]/_/b_[%]", "/a[2]/_/b_[%]");
        assertPathLike("/a[2]/_/b_[%]%", "/a[2]/_/b_[%]%");
        assertPathLike("/a[2]/_/b_[3]%", "/a[2]/_/b_[3]%");
        assertPathLike("/a[2]/_/b_[_]", "/a[2]/_/b_[_]");
        assertPathLike("/a[2]/_/b_[_]%", "/a[2]/_/b_[_]%");
        assertPathLike("/a[2]/_/b_[_]_", "/a[2]/_/b_[_]_");
        assertPathLike("/a[2]/__/b_[_]", "/a[2]/__[1]/b_[_]");
        assertPathLike("/a[2]/___/b_[_]", "/a[2]/___[1]/b_[_]");
        assertPathLike("/a[2]/___/b__[__]", "/a[2]/___[1]/b__[__]");
        assertPathLike("/a[2]/___/__b__[__]", "/a[2]/___[1]/__b__[__]");
        assertPathLike("/a[2]/___/__b__[]", "/a[2]/___[1]/__b__[]");
        assertPathLike("/a[2]/__/b_", "/a[2]/__[1]/b_[1]");
    }

    @Test
    public void shouldCreateCorrectLikeExpressionsForRelativePathsWithAnyWildcard() {
        assertPathLike("%", "%");
        assertPathLike("%/a", "%/a[1]");
        assertPathLike("%%/a", "%/a[1]");
        assertPathLike("%%%/a", "%/a[1]");
        assertPathLike("%/a/b", "%/a[1]/b[1]");
        assertPathLike("%/a[2]", "%/a[2]");
        assertPathLike("%/a[2]/b", "%/a[2]/b[1]");
        assertPathLike("%/a[2]/%/b", "%/a[2]/%/b[1]");
        assertPathLike("%/a[2]/%/b%", "%/a[2]/%/b%[1]");
        assertPathLike("%/a[2]/%/b%[%]", "%/a[2]/%/b%[%]");
        assertPathLike("%/a[2]/%/b%[%]%", "%/a[2]/%/b%[%]%");
        assertPathLike("%/a[2]/%/b%[3]%", "%/a[2]/%/b%[3]%");
    }

    protected void assertPathLike( String inputPath,
                                   String expectedOutputPath ) {
        assertThat(search.likeExpresionForWildcardPath(inputPath), is(expectedOutputPath));
    }

}
