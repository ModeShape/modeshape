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
package org.modeshape.web.jcr;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.modeshape.jcr.api.RepositoryFactory;

public class RepositoryManagerTest {

    private final String VALID_JCR_URL = "file:src/test/resources/repo-config.json";
    protected final List<String> PARAM_NAMES = Collections.singletonList(RepositoryFactory.URL);

    private ServletContext context;

    @Before
    public void beforeEach() throws Exception {
        context = mock(ServletContext.class);
        when(context.getInitParameterNames()).thenAnswer(new Answer<Enumeration<?>>() {
            @SuppressWarnings( {"unchecked", "rawtypes"} )
            @Override
            public Enumeration<?> answer( InvocationOnMock invocation ) throws Throwable {
                return new Vector(PARAM_NAMES).elements();
            }
        });
        when(context.getInitParameter(RepositoryFactory.URL)).thenReturn(VALID_JCR_URL);
    }

    @Test
    public void shouldLoadFileBasedModeShapeEngineFromClassPath() throws Exception {
        // This will start up the repository ...
        RepositoryManager.initialize(context);

        Set<String> repoNames = RepositoryManager.getJcrRepositoryNames();
        assertThat(repoNames, is(Collections.singleton("Test Repository")));

        Session session = RepositoryManager.getSession(null, "Test Repository", "default");
        assertThat(session, is(notNullValue()));

        try {
            String repoName = "xxxyyyzzz";
            RepositoryManager.getSession(null, repoName, "default");
            fail("Should not have found repository \"" + repoName + "\"");
        } catch (NoSuchRepositoryException e) {
            // expected
        }

        RepositoryManager.shutdown();
    }
}
