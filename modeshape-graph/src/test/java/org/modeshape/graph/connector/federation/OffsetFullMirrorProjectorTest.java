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
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.Location;

/**
 * 
 */
public class OffsetFullMirrorProjectorTest extends AbstractProjectorTest<OffsetMirrorProjector> {

    private String mirrorSourceName;
    private String mirrorWorkspaceName;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        this.mirrorSourceName = "source1";
        this.mirrorWorkspaceName = "workspace1";
        addProjection(mirrorSourceName, mirrorWorkspaceName, "/a/b/c => /");
        this.projector = OffsetMirrorProjector.with(context, projections);
    }

    protected void assertProjectedIntoMirror( String federatedPath,
                                              String pathInSource ) {
        Location location = location(federatedPath);
        ProjectedNode node = projector.project(context, location, false);
        assertThat(node.isProxy(), is(true));
        ProxyNode proxy = node.asProxy();
        assertThat(proxy.location().getPath(), is(path(pathInSource)));
        assertThat(proxy.source(), is(mirrorSourceName));
        assertThat(proxy.workspaceName(), is(mirrorWorkspaceName));
        assertThat(proxy.hasNext(), is(false));
    }

    @Test
    public void shouldAlwaysReturnProxyNodeForLocationAboveMirrorSource() {
        assertPlacholderHasChildren("/", "a");
        assertPlacholderHasChildren("/a", "b");
        assertPlacholderHasChildren("/a/b", "c");
        assertProjectedIntoMirror("/a/b/c", "/");
    }

    @Test
    public void shouldAlwaysReturnProxyNodeForLocationWithinMirror() {
        assertProjectedIntoMirror("/a/b/c", "/");
        assertProjectedIntoMirror("/a/b/c/d", "/d");
        assertProjectedIntoMirror("/a/b/c/d/e", "/d/e");
    }

    @Test
    public void shouldReturnNoProjectedNodeForLocationOffPathToSourceBranch() {
        assertNoProjectedNodeAt("/a[2]");
        assertNoProjectedNodeAt("/a/b[2]");
        assertNoProjectedNodeAt("/a/b/c[2]");
    }
}
