package org.modeshape.jcr.perftests;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Horia Chiorean
 */
public final class PerformanceTestSuiteRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTestSuiteRunner.class);

    private PerformanceStatistics perfStatistics;
    private RunnerConfiguration runnerConfig;

    public PerformanceTestSuiteRunner() {
        perfStatistics = new PerformanceStatistics();
        runnerConfig = new RunnerConfiguration("runner.properties");
    }

    public void runPerformanceTests( Map repositoryConfigParams, Credentials credentials ) throws Exception {
        for (RepositoryFactory repositoryFactory : ServiceLoader.load(RepositoryFactory.class)) {
            Repository repository = initializeRepository(repositoryConfigParams, repositoryFactory);
            SuiteConfiguration suiteConfiguration = new SuiteConfiguration(repository, credentials, "testsuite.properties");
            Set<Class<? extends AbstractPerformanceTestSuite>> testSuites = loadPerformanceTestSuites();
            for (Class<? extends AbstractPerformanceTestSuite> testSuiteClass : testSuites) {
                runTestSuite(suiteConfiguration, testSuiteClass);
            }
        }
    }

    private void runTestSuite( SuiteConfiguration suiteConfiguration, Class<? extends AbstractPerformanceTestSuite> testSuiteClass )
            throws Exception {
        AbstractPerformanceTestSuite testSuite = testSuiteClass.getConstructor(SuiteConfiguration.class).newInstance(suiteConfiguration);

        if (isSuiteExcluded(testSuiteClass)) {
            return;
        }

        if (!testSuite.isCompatibleWithCurrentRepository()) {
            LOGGER.warn("Test suite {} not compatible with {}", new Object[]{testSuite.getClass().getSimpleName(),
                    suiteConfiguration.getRepository().getClass().getSimpleName()});
            return;
        }

        for (int i = 0; i < runnerConfig.repeatCount; i++) {
            LOGGER.info("Running {} pass count {}", testSuiteClass.getSimpleName(), i);
            testSuite.setUp();
            long duration = testSuite.run();
            perfStatistics.recordStatisticForRepository(suiteConfiguration.getRepository(), testSuiteClass.getSimpleName(), duration);
            testSuite.tearDown();
        }
    }

    private boolean isSuiteExcluded(Class<? extends AbstractPerformanceTestSuite> testSuiteClass) {
        //first search excluded list
        if (patternMatchesSuiteName(testSuiteClass, runnerConfig.excludeTestsRegExp)) {
            return true;
        }
        //then search included list
        return !runnerConfig.includeTestsRegExp.isEmpty() && !patternMatchesSuiteName(testSuiteClass, runnerConfig.includeTestsRegExp);
    }

    private boolean patternMatchesSuiteName( Class<? extends AbstractPerformanceTestSuite> suiteClass, List<String> patternsList ) {
        for (Iterator<String> iterator = patternsList.iterator(); iterator.hasNext();) {
            String pattern = iterator.next();
            try {
                if (Pattern.matches(pattern, suiteClass.getName()) || Pattern.matches(pattern, suiteClass.getSimpleName())){
                    return true;
                }
            } catch (PatternSyntaxException e) {
                LOGGER.warn("Invalid regex {}", pattern);
                iterator.remove();
            }
        }
        return false;
    }

    private Repository initializeRepository( Map repositoryConfigParams, RepositoryFactory repositoryFactory ) throws RepositoryException {
        long start = System.nanoTime();
        Repository repository = repositoryFactory.getRepository(repositoryConfigParams);
        long initializationTime = System.nanoTime() - start;
        perfStatistics.recordStatisticForRepository(repository, "initialization", initializationTime);
        return repository;
    }

    private Set<Class<? extends AbstractPerformanceTestSuite>> loadPerformanceTestSuites() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        for (String subpackageName : runnerConfig.scanSubPackages) {
            String fullPackageName = this.getClass().getPackage().getName() + "." + subpackageName;
            builder.addUrls(getClass().getClassLoader().getResource(fullPackageName.replaceAll("\\.", "/")));
        }
        Reflections reflections = new Reflections(builder);
        return reflections.getSubTypesOf(AbstractPerformanceTestSuite.class);
    }
}

