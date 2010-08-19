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
import java.util.Set;
import org.junit.Test;

/**
 * The <code>WorkspaceTest</code> class is a test class for the {@link Workspace workspace} object.
 */
public final class NodeTypeTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    private static final String NAME1 = "name1";

    private static final String NAME2 = "name2";

    private static final Server SERVER1 = new Server("file:/tmp/temp.txt/resources", "user", "pswd");

    private static final Server SERVER2 = new Server("http:www.redhat.com/resources", "user", "pswd");

    private static final Repository REPOSITORY1 = new Repository(NAME1, SERVER1);

    private static final Repository REPOSITORY2 = new Repository(NAME2, SERVER2);

    private static final Workspace WORKSPACE1 = new Workspace(NAME1, REPOSITORY1);

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    @Test
    public void shouldBeEqualIfHavingSameProperies() {
        assertThat(WORKSPACE1, equalTo(new Workspace(WORKSPACE1.getName(), WORKSPACE1.getRepository())));
    }

    @Test
    public void shouldHashToSameValueIfEquals() {
        Set<Workspace> set = new HashSet<Workspace>();
        set.add(WORKSPACE1);
        set.add(new Workspace(WORKSPACE1.getName(), WORKSPACE1.getRepository()));
        assertThat(set.size(), equalTo(1));
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullName() {
        new Workspace(null, REPOSITORY1);
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullRepository() {
        new Workspace(NAME1, null);
    }

    @Test
    public void shouldNotBeEqualIfSameNameButDifferentRepository() {
        assertThat(WORKSPACE1, is(not(equalTo(new Workspace(WORKSPACE1.getName(), REPOSITORY2)))));
    }

    @Test
    public void shouldNotBeEqualIfSameRepositoryButDifferentName() {
        assertThat(WORKSPACE1, is(not(equalTo(new Workspace(NAME2, WORKSPACE1.getRepository())))));
    }

}
