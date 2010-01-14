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
import org.modeshape.graph.Location;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class BranchedMirrorProjectorTest extends AbstractProjectorTest<BranchedMirrorProjector> {

    private String mirrorSourceName;
    private String mirrorWorkspaceName;
    private String branchSourceName;
    private String branchWorkspaceName;

    @Before
    @Override
    public void beforeEach() {
        super.beforeEach();
        this.mirrorSourceName = "source1";
        this.mirrorWorkspaceName = "workspace1";
        this.branchSourceName = "source2";
        this.branchWorkspaceName = "workspace2";
        addProjection(mirrorSourceName, mirrorWorkspaceName, "/ => /");
        addProjection(branchSourceName, branchWorkspaceName, "/system => /system");
        this.projector = BranchedMirrorProjector.with(context, projections);
    }

    protected void assertProjectedIntoMirror( String federatedPath ) {
        Location location = location(federatedPath);
        ProjectedNode node = projector.project(context, location, false);
        assertThat(node.isProxy(), is(true));
        ProxyNode proxy = node.asProxy();
        assertThat(proxy.location(), is(location));
        assertThat(proxy.source(), is(mirrorSourceName));
        assertThat(proxy.workspaceName(), is(mirrorWorkspaceName));
        assertThat(proxy.hasNext(), is(false));
    }

    protected void assertProjectedIntoBranch( String federatedPath ) {
        assertProjectedIntoBranch(federatedPath, federatedPath);
    }

    protected void assertProjectedIntoBranch( String federatedPath,
                                              String expectedPathInSource ) {
        Location location = location(federatedPath);
        ProjectedNode node = projector.project(context, location, false);
        assertThat(node.isProxy(), is(true));
        ProxyNode proxy = node.asProxy();
        assertThat(proxy.source(), is(branchSourceName));
        assertThat(proxy.workspaceName(), is(branchWorkspaceName));
        assertThat(proxy.location().getPath(), is(path(expectedPathInSource)));
        assertThat(proxy.hasNext(), is(false));
    }

    @Test
    public void shouldAlwaysReturnProxyNodeForLocationInMirrorSource() {
        assertProjectedIntoMirror("/a");
        assertProjectedIntoMirror("/a/b/c/d");
        assertProjectedIntoMirror("/system[2]/b/c/d"); // technically not in the branch!
    }

    @Test
    public void shouldAlwaysReturnProxyNodeForLocationInBranchOnlyOneLevelDeepWithOneForOneMapping() {
        assertProjectedIntoBranch("/system");
        assertProjectedIntoBranch("/system[1]");
        assertProjectedIntoBranch("/system/d");
        assertProjectedIntoBranch("/system[1]/d");
    }

    @Test
    public void shouldReturnForRootLocationAProxyForMirrorAndPlaceholderForBranch() {
        Location location = location("/");
        ProjectedNode node = projector.project(context, location, false);
        assertThat(node.isProxy(), is(true));
        ProxyNode proxy = node.asProxy();
        assertThat(proxy.source(), is(mirrorSourceName));
        assertThat(proxy.workspaceName(), is(mirrorWorkspaceName));
        assertThat(proxy.location().getPath(), is(path("/")));
        assertThat(proxy.hasNext(), is(true));

        ProjectedNode next = node.next();
        assertThat(next.isPlaceholder(), is(true));
        PlaceholderNode placeholder = next.asPlaceholder();
        assertThat(placeholder.isPlaceholder(), is(true));
        assertThat(placeholder.location().getPath(), is(path("/")));
        assertThat(placeholder.children().size(), is(1));
        assertThat(placeholder.children().get(0).location().getPath(), is(path("/system")));
        assertThat(placeholder.hasNext(), is(false));
    }

    @Test
    public void shouldAlwaysReturnProxyNodeForLocationInBranchMultipleLevelsDeepWithOneForOneMapping() {
        projections.clear();
        addProjection(mirrorSourceName, mirrorWorkspaceName, "/ => /");
        addProjection(branchSourceName, branchWorkspaceName, "/a/b/c => /a/b/c");
        this.projector = BranchedMirrorProjector.with(context, projections);
        assertProjectedIntoBranch("/a/b/c");
        assertProjectedIntoBranch("/a/b/c/d");
    }

    @Test
    public void shouldAlwaysReturnProxyNodeForLocationInBranchMultipleLevelsDeepWithDissimilarMapping() {
        projections.clear();
        addProjection(mirrorSourceName, mirrorWorkspaceName, "/ => /");
        addProjection(branchSourceName, branchWorkspaceName, "/a/b/c => /d/e");
        this.projector = BranchedMirrorProjector.with(context, projections);
        assertProjectedIntoBranch("/a/b/c", "/d/e");
        assertProjectedIntoBranch("/a/b/c/f", "/d/e/f");
    }

    @Test
    public void shouldAlwaysReturnProxyNodeForLocationInBranchMultipleLevelsDeepWithRootMapping() {
        projections.clear();
        addProjection(mirrorSourceName, mirrorWorkspaceName, "/ => /");
        addProjection(branchSourceName, branchWorkspaceName, "/a/b/c => /");
        this.projector = BranchedMirrorProjector.with(context, projections);
        assertProjectedIntoBranch("/a/b/c", "/");
        assertProjectedIntoBranch("/a/b/c/f", "/f");
    }
}
