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
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.federation.Projection.Rule;
import org.modeshape.graph.property.Path;

/**
 * @param <ProjectorType>
 */
public abstract class AbstractProjectorTest<ProjectorType extends Projector> {

    protected ExecutionContext context;
    protected List<Projection> projections;
    protected ProjectorType projector;
    protected Location locationA;
    protected Location locationB;
    protected Location locationAB;
    protected Location locationABCD;

    @Before
    public void beforeEach() {
        this.context = new ExecutionContext();
        this.projections = new ArrayList<Projection>();
        this.locationA = Location.create(path("/a"));
        this.locationB = Location.create(path("/b"));
        this.locationAB = Location.create(path("/a/b"));
        this.locationABCD = Location.create(path("/a/b/c/d"));
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

    protected void clearProjections() {
        projections.clear();
    }

    protected Path.Segment segment( String segment ) {
        return context.getValueFactories().getPathFactory().createSegment(segment);
    }

    protected Location location( String path ) {
        return Location.create(path(path));
    }

    protected Location location( UUID uuid ) {
        return Location.create(uuid);
    }

    protected void assertNoProjectedNodeAt( String path ) {
        ProjectedNode node = projector.project(context, location(path), false);
        assertThat(node, is(nullValue()));
    }

    protected void assertPlacholderHasChildren( String parent,
                                                String... childSegments ) {
        Path parentPath = path(parent);
        ProjectedNode node = projector.project(context, Location.create(parentPath), false);
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
}
