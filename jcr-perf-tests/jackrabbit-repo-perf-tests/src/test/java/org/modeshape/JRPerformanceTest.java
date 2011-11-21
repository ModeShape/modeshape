package org.modeshape;

import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.commons.JcrUtils;
import org.junit.Test;
import org.modeshape.jcr.perftests.PerformanceTestSuiteRunner;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Horia Chiorean
 */
public class JRPerformanceTest {

    @Test
    public void testPerformance() throws Exception {
        Map<String, URL> parameters = new HashMap<String, URL>();
        parameters.put(JcrUtils.REPOSITORY_URI, getClass().getClassLoader().getResource("./"));
        new PerformanceTestSuiteRunner().runPerformanceTests(parameters, new SimpleCredentials("test", "test".toCharArray()));
    }
}
