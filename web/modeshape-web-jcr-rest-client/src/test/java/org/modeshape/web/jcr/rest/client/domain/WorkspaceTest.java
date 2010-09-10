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
package org.modeshape.web.jcr.rest.client.domain;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.junit.Test;

/**
 * The <code>WorkspaceTest</code> class is a test class for the {@link Workspace workspace} object.
 */
public final class WorkspaceTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    private static final String NAME1 = "nt:file";

    private static final String NAME2 = "nt:resource";

    private static final Server SERVER1 = new Server("file:/tmp/temp.txt/resources", "user", "pswd");

    private static final Repository REPOSITORY1 = new Repository(NAME1, SERVER1);

    private static final Workspace WORKSPACE1 = new Workspace(NAME1, REPOSITORY1);
    private static final Workspace WORKSPACE2 = new Workspace(NAME2, REPOSITORY1);
    
    private static final NodeType NODETYPE1 = new NodeType(NAME1, WORKSPACE1, null);
    
    private static final NodeType NODETYPE2 = new NodeType(NAME1, WORKSPACE1, new Properties());
    

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    @Test
    public void shouldBeEqualIfHavingSameProperies() {
        assertThat(NODETYPE2, equalTo(new NodeType(NAME1, WORKSPACE1, new Properties())));
    }

    @Test
    public void shouldHashToSameValueIfEquals() {
        Set<NodeType> set = new HashSet<NodeType>();
        set.add(NODETYPE1);
        set.add(new NodeType("nt:file", WORKSPACE1, null));
        assertThat(set.size(), equalTo(1));
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullName() {
        new NodeType(null, WORKSPACE1, new Properties());
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullRepository() {
        new NodeType(NAME1, null, null);
    }

    @Test
    public void shouldNotBeEqualIfSameNameButDifferentWorkspace() {
        assertThat(NODETYPE2, is(not(equalTo(new NodeType(NAME1, WORKSPACE2, new Properties())))));
    }

    @Test
    public void shouldNotBeEqualIfSameWorkspaceButDifferentName() {
        assertThat(NODETYPE2, is(not(equalTo(new NodeType(NAME2, WORKSPACE1, new Properties())))));
    }

}
