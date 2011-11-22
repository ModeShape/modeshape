package org.modeshape;

import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.commons.JcrUtils;
import org.junit.Test;
import org.modeshape.jcr.perftests.PerformanceTestSuiteRunner;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Test which runs the performance suite against a Jackrabbit in memory repo.
 *
 * @author Horia Chiorean
 */
public class JRPerformanceTest {

    @Test
    public void testJackrabbitInMemoryRepo() throws Exception {
        Map<String, URL> parameters = new HashMap<String, URL>();
        parameters.put(JcrUtils.REPOSITORY_URI, getClass().getClassLoader().getResource("./"));
        new PerformanceTestSuiteRunner().runPerformanceTests(parameters, new SimpleCredentials("test", "test".toCharArray()));
    }
}
