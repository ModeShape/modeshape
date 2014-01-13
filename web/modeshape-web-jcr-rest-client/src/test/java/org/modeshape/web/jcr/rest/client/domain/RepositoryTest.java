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
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * The <code>RepositoryTest</code> class is a test class for the {@link Repository repository} object.
 */
public final class RepositoryTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    private static final String NAME1 = "name1";

    private static final String NAME2 = "name2";

    private static final Server SERVER1 = new Server("file:/tmp/temp.txt/resources", "user", "pswd");

    private static final Server SERVER2 = new Server("http:www.redhat.com/resources",  "user", "pswd");

    private static final Repository REPOSITORY1 = new Repository(NAME1, SERVER1);

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    @Test
    public void shouldBeEqualIfHavingSameProperies() {
        assertThat(REPOSITORY1, equalTo(new Repository(REPOSITORY1.getName(), REPOSITORY1.getServer())));
    }

    @Test
    public void shouldHashToSameValueIfEquals() {
        Set<Repository> set = new HashSet<Repository>();
        set.add(REPOSITORY1);
        set.add(new Repository(REPOSITORY1.getName(), REPOSITORY1.getServer()));
        assertThat(set.size(), equalTo(1));
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullName() {
        new Repository(null, SERVER1);
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullServer() {
        new Repository(NAME1, null);
    }

    @Test
    public void shouldNotBeEqualIfSameNameButDifferentServers() {
        assertThat(REPOSITORY1, is(not(equalTo(new Repository(REPOSITORY1.getName(), SERVER2)))));
    }

    @Test
    public void shouldNotBeEqualIfSameServerButDifferentName() {
        assertThat(REPOSITORY1, is(not(equalTo(new Repository(NAME2, REPOSITORY1.getServer())))));
    }

}
