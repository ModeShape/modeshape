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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.federation.Projection.Rule;
import org.modeshape.graph.property.Path;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ProjectorWithPlaceholdersTest {

    protected ExecutionContext context;
    protected List<Projection> projections;
    protected Collection<PlaceholderNode> placeholders;

    @Before
    public void beforeEach() {
        this.context = new ExecutionContext();
        this.projections = new ArrayList<Projection>();
        this.placeholders = new ArrayList<PlaceholderNode>();
    }

    protected Rule[] rule( String... rule ) {
        Rule[] rules = new Rule[rule.length];
        for (int i = 0; i != rule.length; ++i) {
            rules[i] = Projection.fromString(rule[i], context);
        }
        return rules;
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Path path( Path parent,
                         String relativePath ) {
        return context.getValueFactories().getPathFactory().create(parent, relativePath);
    }

    protected void addProjection( String sourceName,
                                  String workspaceName,
                                  String... rules ) {
        projections.add(new Projection(sourceName, workspaceName, false, rule(rules)));
    }

    protected PlaceholderNode getPlaceholder( Path path ) {
        for (PlaceholderNode placeholder : placeholders) {
            if (placeholder.location().getPath().equals(path)) return placeholder;
        }
        return null;
    }

    protected void assertPlacholderHasChildren( String parent,
                                                String... childSegments ) {
        Path parentPath = path(parent);
        ProjectedNode node = getPlaceholder(parentPath);
        assertThat(node, is(notNullValue()));
        assertThat(node.isPlaceholder(), is(true));
        PlaceholderNode placeholder = node.asPlaceholder();
        List<Path> locations = new ArrayList<Path>();
        for (String childSegment : childSegments) {
            Path childPath = path(parentPath, childSegment);
            locations.add(childPath);
        }
        List<Path> actual = new ArrayList<Path>();
        for (ProjectedNode child : placeholder.children()) {
            if (child.isPlaceholder()) {
                actual.add(child.location().getPath());
            } else {
                actual.add(child.asProxy().federatedLocation().getPath());
            }
        }
        assertThat(actual, is(locations));
    }

    protected void assertNoPlacholder( String parent ) {
        assertThat(getPlaceholder(path(parent)), is(nullValue()));
    }

    @Test
    public void shouldLoadPlaceholderNodesForWorkspaceWithOneProjectionNotAtRoot() {
        addProjection("source1", "workspace1", "/a/b => /c/d");
        ProjectorWithPlaceholders.loadPlaceholderNodes(context, projections, placeholders);
        assertPlacholderHasChildren("/", "a");
        assertPlacholderHasChildren("/a", "b");
        assertThat(placeholders.size(), is(2));
    }

    @Test
    public void shouldLoadPlaceholderNodesForWorkspaceWithOneProjectionAtRoot() {
        addProjection("source1", "workspace1", "/ => /");
        ProjectorWithPlaceholders.loadPlaceholderNodes(context, projections, placeholders);
        assertNoPlacholder("/");
        assertThat(placeholders.size(), is(0));
    }

    @Test
    public void shouldLoadPlaceholderNodesForWorkspaceWithMultipleProjectionsNotAtRoot() {
        addProjection("source1", "workspace1", "/a/b => /a1/b1");
        addProjection("source2", "workspace2", "/c/d => /c1/d1");
        ProjectorWithPlaceholders.loadPlaceholderNodes(context, projections, placeholders);
        assertPlacholderHasChildren("/", "a", "c");
        assertPlacholderHasChildren("/a", "b");
        assertPlacholderHasChildren("/c", "d");
        assertThat(placeholders.size(), is(3));
    }

    @Test
    public void shouldLoadPlaceholderNodesForWorkspaceWithMultipleProjectionsAtRoot() {
        addProjection("source1", "workspace1", "/ => /");
        addProjection("source2", "workspace2", "/ => /");
        ProjectorWithPlaceholders.loadPlaceholderNodes(context, projections, placeholders);
        assertNoPlacholder("/");
        assertThat(placeholders.size(), is(0));
    }

    @Test
    public void shouldLoadPlaceholderNodesForWorkspaceWithMirrorAndBranchProjections() {
        addProjection("source1", "workspace1", "/ => /");
        addProjection("source2", "workspace2", "/a => /a");
        ProjectorWithPlaceholders.loadPlaceholderNodes(context, projections, placeholders);
        assertPlacholderHasChildren("/", "a"); // placeholder for root contains only branch root
        assertThat(placeholders.size(), is(1));
        assertNoPlacholder("/a");
        assertNoPlacholder("/b");
        assertNoPlacholder("/c/d");
        assertNoPlacholder("/a/e");
    }

    @Test
    public void shouldLoadPlaceholderNodesForWorkspaceWithMirrorAndOffsetBranchProjections() {
        addProjection("source1", "workspace1", "/ => /");
        addProjection("source2", "workspace2", "/a/b/c => /a/b");
        ProjectorWithPlaceholders.loadPlaceholderNodes(context, projections, placeholders);
        assertPlacholderHasChildren("/", "a"); // placeholder for root contains only branch root
        assertPlacholderHasChildren("/a", "b");
        assertPlacholderHasChildren("/a/b", "c");
        assertThat(placeholders.size(), is(3));
        assertNoPlacholder("/x");
        assertNoPlacholder("/y/z");
        assertNoPlacholder("/a/b/c");
    }

    @Test
    public void shouldLoadPlaceholderNodesForWorkspaceWithOffsetMirrorProjection() {
        addProjection("source1", "workspace1", "/a/b/c => /");
        ProjectorWithPlaceholders.loadPlaceholderNodes(context, projections, placeholders);
        assertPlacholderHasChildren("/", "a"); // placeholder for root contains only branch root
        assertPlacholderHasChildren("/a", "b");
        assertPlacholderHasChildren("/a/b", "c");
        assertThat(placeholders.size(), is(3));
        assertNoPlacholder("/a[2]");
        assertNoPlacholder("/a/b[2]");
        assertNoPlacholder("/a/b/c[2]");
    }

}
