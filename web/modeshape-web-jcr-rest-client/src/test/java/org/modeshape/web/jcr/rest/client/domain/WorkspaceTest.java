/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
