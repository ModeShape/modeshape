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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.value.binary.BinaryStore;

public class ExecutionContextTest {

    private ExecutionContext context;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
    }

    @After
    public void afterEach() {
        context = null;
    }

    @Test
    public void shouldHaveIdentifierThatIsNotNull() {
        assertThat(context.getId(), is(notNullValue()));
    }

    @Test
    public void shouldHaveIdentifierThatIsNotBlank() {
        assertThat(context.getId().length(), is(not(0)));
        assertThat(context.getId().trim().length(), is(not(0)));
    }

    @Test
    public void shouldHaveIdentifierThatIsUnique() {
        // Can't really test this, but we certainly can test that there are no duplicates in many contexts ...
        Set<String> ids = new HashSet<String>();
        for (int i = 0; i != 50; ++i) {
            assertThat(ids.add(new ExecutionContext().getId()), is(true));
        }
    }

    @Test
    public void shouldCreateSubcontextsWithDifferentIdentifiers() {
        ExecutionContext newContext = context.with(mock(BinaryStore.class));
        assertThat(newContext.getId(), is(not(context.getId())));
    }
}
