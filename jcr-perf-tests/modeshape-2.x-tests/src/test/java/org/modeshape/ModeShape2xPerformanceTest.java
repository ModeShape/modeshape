package org.modeshape;

import org.junit.Test;
import org.modeshape.jcr.JcrRepositoryFactory;
import org.modeshape.jcr.perftests.PerformanceTestSuiteRunner;
import org.modeshape.jcr.perftests.RunnerConfiguration;
import org.modeshape.jcr.perftests.query.ThreeWayJoinTestSuite;
import org.modeshape.jcr.perftests.query.TwoWayJoinTestSuite;
import org.modeshape.jcr.perftests.read.BigFileReadTestSuite;
import org.modeshape.jcr.perftests.read.SmallFileReadTestSuite;
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
        //TODO author=Horia Chiorean date=11/22/11 description=for some reason, modeshape crashes in the join tests & binary incompatibility
        RunnerConfiguration runnerConfig = new RunnerConfiguration().addExcludeTests(
                TwoWayJoinTestSuite.class.getSimpleName(),
                ThreeWayJoinTestSuite.class.getSimpleName(),
                BigFileReadTestSuite.class.getSimpleName(),
                SmallFileReadTestSuite.class.getSimpleName());
        new PerformanceTestSuiteRunner(runnerConfig).runPerformanceTests(parameters, null);
    }
}
