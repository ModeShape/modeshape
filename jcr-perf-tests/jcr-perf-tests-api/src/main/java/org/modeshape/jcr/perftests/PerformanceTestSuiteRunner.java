package org.modeshape.jcr.perftests;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Horia Chiorean
 */
public final class PerformanceTestSuiteRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTestSuiteRunner.class);

    private PerformanceStatistics perfStatistics;
    private List<String> excludeTestsRegExp;
    private List<String> scanSubPackages;
    private int repeatCount;

    public PerformanceTestSuiteRunner() {
        perfStatistics = new PerformanceStatistics();
        excludeTestsRegExp = new ArrayList<String>();
        scanSubPackages = new ArrayList<String>();
        repeatCount = 1;
        initFromConfigFile();
    }

    private void initFromConfigFile() {
        try {
            Properties configParams = new Properties();
            configParams.load(getClass().getClassLoader().getResourceAsStream("perftests.properties"));
            parseMultiValuedString(configParams.getProperty("tests.exclude"), excludeTestsRegExp);
            parseMultiValuedString(configParams.getProperty("scan.subPackages"), scanSubPackages);
            repeatCount = Integer.valueOf(configParams.getProperty("repeat.count"));
        } catch (IOException e) {
            LOGGER.warn("Cannot load config file. Will use defaults ", e);
        }
    }

    private void parseMultiValuedString( String multiValueString, List<String> collector) {
        if (multiValueString == null) {
            return;
        }
        String[] values = multiValueString.split(",");
        for (String value : values) {
            if (!value.trim().isEmpty()) {
                collector.add(value.trim());
            }
        }
    }

    public void runPerformanceTests( Map repositoryConfigParams, Credentials credentials ) throws Exception {
        for (RepositoryFactory repositoryFactory : ServiceLoader.load(RepositoryFactory.class)) {
            Repository repository = initializeRepository(repositoryConfigParams, repositoryFactory);
            Set<Class<? extends AbstractPerformanceTestSuite>> perfTestSuites = loadPerformanceTestSuites();
            for (Class<? extends AbstractPerformanceTestSuite> testSuiteClass : perfTestSuites) {
                runPerfTestSuite(credentials, repository, testSuiteClass);
            }
        }
    }

    private void runPerfTestSuite( Credentials credentials, Repository repository, Class<? extends AbstractPerformanceTestSuite> testSuiteClass ) throws Exception {
        AbstractPerformanceTestSuite testSuite = testSuiteClass.newInstance();
        String testSuiteShortName = testSuite.getClass().getSimpleName();
        String testSuiteFullName = testSuite.getClass().getName();

        if (!testSuite.isCompatibleWith(repository)) {
            LOGGER.warn("{} not compatible with {}", testSuiteShortName, repository.getClass().getSimpleName());
            return;
        }

        for (Iterator<String> it = excludeTestsRegExp.iterator(); it.hasNext();) {
            String pattern = it.next();
            try {
                if (Pattern.matches(pattern, testSuiteFullName) || Pattern.matches(pattern, testSuiteShortName)){
                    LOGGER.info("{} will not because it is excluded by {}", testSuiteFullName, pattern);
                }
            } catch (PatternSyntaxException e) {
                LOGGER.warn("Invalid regex {}", pattern);
                it.remove();
            }
        }

        for (int i = 0; i < repeatCount; i++) {
            testSuite.setUp(repository, credentials);
            long duration = testSuite.run();
            testSuite.tearDown();
            perfStatistics.recordStatisticForRepository(repository, testSuiteShortName, duration);
        }
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
        for (String subpackageName : scanSubPackages) {
            String fullPackageName = this.getClass().getPackage().getName() + "." + subpackageName;
            builder.addUrls(getClass().getClassLoader().getResource(fullPackageName.replaceAll("\\.", "/")));
        }
        Reflections reflections = new Reflections(builder);
        return reflections.getSubTypesOf(AbstractPerformanceTestSuite.class);
    }
}

