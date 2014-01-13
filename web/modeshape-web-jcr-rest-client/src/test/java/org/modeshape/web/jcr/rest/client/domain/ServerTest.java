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
 * The <code>ServerTest</code> class is a test class for the {@link Server server} object.
 */
public final class ServerTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    private static final String URL1 = "file:/tmp/temp.txt/resources";
    private static final String URL2 = "http:www.redhat.com/resources";

    private static final String USER1 = "user1";
    private static final String USER2 = "user2";

    private static final String PSWD1 = "pwsd1";
    private static final String PSWD2 = "pwsd2";

    private static Server SERVER1 = new Server(URL1, USER1, PSWD1);
    private static Server SERVER2 = new Server(URL2, USER2, PSWD2);

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    @Test
    public void shouldBeEqualIfHavingSameProperies() {
        assertThat(SERVER1, equalTo(new Server(SERVER1.getUrl(), SERVER1.getUser(), SERVER1.getPassword())));
        assertThat(SERVER2, equalTo(new Server(SERVER2.getUrl(), SERVER2.getUser(), SERVER2.getPassword())));
    }

    @Test
    public void shouldHashToSameValueIfEquals() {
        Set<Server> set = new HashSet<Server>();
        set.add(SERVER1);
        set.add(new Server(SERVER1.getUrl(), SERVER1.getUser(), SERVER1.getPassword()));
        assertThat(set.size(), equalTo(1));
    }

    @Test( expected = java.lang.AssertionError.class )
    public void shouldNotAllowNullUrl() {
        new Server(null, USER1, PSWD1);
    }

    @Test
    public void shouldHaveSameKey() {
        assertThat(SERVER1.hasSameKey(new Server(SERVER1.getUrl(), SERVER1.getUser(), SERVER1.getPassword())), is(true));
    }

    @Test
    public void shouldNotBeEqualIfPropertiesAreDifferent() {
        // different URL
        assertThat(SERVER1, is(not(equalTo(new Server(URL2, SERVER1.getUser(), SERVER1.getPassword())))));

        // different user
        assertThat(SERVER1, is(not(equalTo(new Server(SERVER1.getUrl(), USER2, SERVER1.getPassword())))));

        // different password
        assertThat(SERVER1, is(not(equalTo(new Server(SERVER1.getUrl(), SERVER1.getUser(), PSWD2)))));
    }

    @Test
    public void shouldNotHaveSameKey() {
        assertThat(SERVER1.hasSameKey(new Server(URL2, SERVER1.getUser(), SERVER1.getPassword())), is(false));
        assertThat(SERVER1.hasSameKey(new Server(SERVER1.getUrl(), USER2, SERVER1.getPassword())), is(false));
    }

}
