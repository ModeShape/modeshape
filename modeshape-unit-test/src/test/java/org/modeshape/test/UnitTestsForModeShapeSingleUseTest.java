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
package org.modeshape.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.Session;
import org.junit.Test;

public class UnitTestsForModeShapeSingleUseTest extends ModeShapeSingleUseTest {

    @Test
    public void shouldAllowCreatingSessions() throws Exception {
        Session session = session();
        assertThat(session.isLive(), is(true));
        session.getRootNode().addNode("topLevel", "nt:unstructured");
        session.save();
    }

    @Test
    public void shouldBeEmpty() throws Exception {
        Session session = session();
        assertThat(session.isLive(), is(true));
        assertThat(session.getRootNode().getNodes().getSize(), is(1L));
        assertThat(session.getRootNode().getNode("jcr:system"), is(notNullValue()));
    }

}
