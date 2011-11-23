package org.modeshape;

import org.junit.Test;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.perftests.PerformanceTestSuiteRunner;
import org.modeshape.jcr.perftests.RunnerConfiguration;
import org.modeshape.jcr.perftests.read.BigFileReadTestSuite;
import org.modeshape.jcr.perftests.read.ConcurrentReadTestSuite;
import org.modeshape.jcr.perftests.read.SmallFileReadTestSuite;
import org.modeshape.jcr.perftests.write.ConcurrentReadWriteTestSuite;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs the performance tests against a Modeshape 2.x repo.
 *
 * @author Horia Chiorean
 */
public class ModeShape2xPerformanceTest {

    @Test
    public void testModeShapeInMemory() throws Exception {
        Map<String, URL> parameters = new HashMap<String, URL>();
        parameters.put(JcrRepositoryFactory.URL, getClass().getClassLoader().getResource("configRepository.xml"));
        //TODO author=Horia Chiorean date=11/22/11 description=some tests excluded because of various problems
        RunnerConfiguration runnerConfig = new RunnerConfiguration().addTestsToExclude(
                ConcurrentReadTestSuite.class.getSimpleName(),//deadlock
                ConcurrentReadWriteTestSuite.class.getSimpleName(),//deadlock
                BigFileReadTestSuite.class.getSimpleName(), //binary incompatibility
                SmallFileReadTestSuite.class.getSimpleName()); //binary incompatibility
        new PerformanceTestSuiteRunner(runnerConfig).runPerformanceTests(parameters, null);
    }
}
