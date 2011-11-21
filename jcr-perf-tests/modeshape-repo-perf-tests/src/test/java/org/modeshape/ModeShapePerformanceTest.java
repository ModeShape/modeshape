package org.modeshape;

import org.junit.Test;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.perftests.PerformanceTestSuiteRunner;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs the performance tests against a Modeshape repo.
 *
 * @author Horia Chiorean
 */
public class ModeShapePerformanceTest {

    @Test
    public void testPerformance() throws Exception {
        Map<String, URL> parameters = new HashMap<String, URL>();
        parameters.put(JcrRepositoryFactory.URL, getClass().getClassLoader().getResource("configRepository.xml"));
        new PerformanceTestSuiteRunner().runPerformanceTests(parameters, null);
    }
}
