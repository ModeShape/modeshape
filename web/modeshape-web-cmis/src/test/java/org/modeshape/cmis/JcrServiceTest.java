
package org.modeshape.cmis;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;

import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.jcr.JcrRepository;
import org.junit.Test;

public class JcrServiceTest {

    /**
     * Ensure that {@link JcrService#workspace(String) returns the workspace
     * for both the short and long form repository ids.
     * 
     * For example, "foo" should return "foo", and "foo:bar" should return "bar".
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
        verify(mockRepo).login(any(Credentials.class), eq("foo"));

        js.login("foo:bar");
        verify(mockRepo).login(any(Credentials.class), eq("bar"));

        js.login("baz:");
        verify(mockRepo).login(any(Credentials.class), eq(""));

        js.login(":quux");
        verify(mockRepo).login(any(Credentials.class), eq("quux"));

        js.login("a:b:c");
        verify(mockRepo).login(any(Credentials.class), eq("b"));
    }
}
