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
package org.modeshape.cmis;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Credentials;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.jcr.JcrRepository;
import org.junit.Test;

public class JcrServiceTest {

    /**
     * Ensure that {@link JcrService} returns the workspace for the long form repository ids (e.g. "repositoryName:workspace") and
     * {@code null} for the short form (e.g. "repositoryName"). For example, "foo" should return {@code null}, and "foo:bar"
     * should return "bar".
     */
    @Test
    public void testWorkspace() {
        String[] names = {"foo", "", "baz", "a"};
        JcrRepository mockRepo = mock(JcrRepository.class);
        Map<String, JcrRepository> jrs = new HashMap<String, JcrRepository>();
        for (String name : names) {
            jrs.put(name, mockRepo);
        }

        JcrService js = new JcrService(jrs);
        js.setCallContext(mock(CallContext.class));

        js.login("foo");
        verify(mockRepo).login(null, null);

        js.login("foo:");
        verify(mockRepo, times(2)).login(null, null);

        js.login("foo:bar");
        verify(mockRepo).login(any(Credentials.class), eq("bar"));

        js.login(":quux");
        verify(mockRepo).login(any(Credentials.class), eq("quux"));

        js.login("a:b:c");
        verify(mockRepo).login(any(Credentials.class), eq("b"));
    }
}
