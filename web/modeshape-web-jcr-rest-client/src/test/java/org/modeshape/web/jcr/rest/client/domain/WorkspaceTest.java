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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

/**
 * The <code>WorkspaceTest</code> class is a test class for the {@link Workspace workspace} object.
 */
public final class WorkspaceTest {

    private static final String NAME1 = "nt:file";
    private static final String NAME2 = "nt:resource";
    private static final Server SERVER1 = new Server("file:/tmp/temp.txt/resources", "user", "pswd");

    private Repository repository;
    private Workspace workspace1;
    private Workspace workspace2;

    @Before
    public void beforeEach() {
        repository = new Repository(NAME1, SERVER1);
        workspace1 = new Workspace(NAME1, repository);
        workspace2 = new Workspace(NAME2, repository);
    }

    @Test
    public void shouldHaveRepository() {
        assertThat(workspace1.getRepository(), is(sameInstance(repository)));
        assertThat(workspace2.getRepository(), is(sameInstance(repository)));
    }

    @Test
    public void shouldHaveName() {
        assertThat(workspace1.getName(), is(NAME1));
        assertThat(workspace2.getName(), is(NAME2));
    }
}
