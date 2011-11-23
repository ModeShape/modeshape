/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.perftests;

import javax.jcr.*;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Class which runs a set of test suites against all the JCR repositories which are found in the classpath. The <code>ServiceLoader</code>
 * mechanism is used for scanning the <code>RepositoryFactory</code> instances.
 *
 * In order to locate the test suites which are to be run, a set of properties (loaded from a config file - runner.properties)
 * have meaning:
 * -scan.subPackages : a comma separated list of simple package names under org.modeshape.jcr.perftests which will be scanned
 * to determine the list of suites to run.
 * -tests.exclude : a comma separated list of java regexps for tests which should be excluded (has precedence of tests.include)
 * -tests.include : a comma separated list of java regexps for tests which should be included
 *
 * @author Horia Chiorean
 */
public final class PerformanceTestSuiteRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTestSuiteRunner.class);

    private PerformanceData perfData;
    private RunnerConfiguration runnerConfig;

    /**
     * Creates a new default test runner instance, which loads its properties from a file called "runner.properties" in the classpath.
     */
    public PerformanceTestSuiteRunner() {
        this(new RunnerConfiguration());
    }

    /**
     * Creates a new runner instance passing a custom config.
     */
    public PerformanceTestSuiteRunner( RunnerConfiguration runnerConfig ) {
        this.perfData = new PerformanceData(TimeUnit.MILLISECONDS);
        this.runnerConfig = runnerConfig;
    }

    /**
     * Uses the given map of parameters together with the <code>ServiceLoader</code> mechanism to get all the <code>RepositoryFactory</code>
     * instances and the subsequent repositories against which the tests will be run.
     *
     * @param repositoryConfigParams a map of config params {@see {@link RepositoryFactory#getRepository(java.util.Map)}}
     * @param credentials a set of credentials which may be needed by a certain repo to run. It can be null.
     */
    public void runPerformanceTests( Map repositoryConfigParams, Credentials credentials ) throws Exception {
        for (RepositoryFactory repositoryFactory : ServiceLoader.load(RepositoryFactory.class)) {
            Repository repository = repositoryFactory.getRepository(repositoryConfigParams);
            if (repository == null) {
                continue;
            }
            initializeRepository(repository, credentials);

            SuiteConfiguration suiteConfiguration = new SuiteConfiguration(repository, credentials, "testsuite.properties");
            Set<Class<? extends AbstractPerformanceTestSuite>> testSuites = loadPerformanceTestSuites();
            for (Class<? extends AbstractPerformanceTestSuite> testSuiteClass : testSuites) {
                runTestSuite(suiteConfiguration, testSuiteClass);
            }
        }
        perfData.print5NrSummary();
    }

    private void runTestSuite( SuiteConfiguration suiteConfiguration, Class<? extends AbstractPerformanceTestSuite> testSuiteClass )
            throws Exception {
        final AbstractPerformanceTestSuite testSuite = testSuiteClass.getConstructor(SuiteConfiguration.class).newInstance(suiteConfiguration);

        if (isSuiteExcluded(testSuiteClass)) {
            return;
        }

        if (!testSuite.isCompatibleWithCurrentRepository()) {
            LOGGER.warn("Test suite {} not compatible with {}", new Object[] {testSuite.getClass().getSimpleName(),
                    suiteConfiguration.getRepository().getClass().getSimpleName()});
            return;
        }

        LOGGER.info("Starting suite: {}", testSuiteClass.getSimpleName());
        testSuite.setUp();
        runAndRecord(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                testSuite.run();
                return null;
            }
        }, suiteConfiguration.getRepository(), testSuiteClass.getSimpleName());
        testSuite.tearDown();
    }

    private boolean isSuiteExcluded( Class<? extends AbstractPerformanceTestSuite> testSuiteClass ) {
        //first search excluded list
        if (patternMatchesSuiteName(testSuiteClass, runnerConfig.excludeTestsRegExp)) {
            return true;
        }
        //then search included list
        return !runnerConfig.includeTestsRegExp.isEmpty() && !patternMatchesSuiteName(testSuiteClass, runnerConfig.includeTestsRegExp);
    }

    private boolean patternMatchesSuiteName( Class<? extends AbstractPerformanceTestSuite> suiteClass, List<String> patternsList ) {
        for (Iterator<String> iterator = patternsList.iterator(); iterator.hasNext(); ) {
            String pattern = iterator.next();
            try {
                if (Pattern.matches(pattern, suiteClass.getName()) || Pattern.matches(pattern, suiteClass.getSimpleName())) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                LOGGER.warn("Invalid regex {}", pattern);
                iterator.remove();
            }
        }
        return false;
    }

    private void initializeRepository(final Repository repository, final Credentials credentials ) throws Exception {
        runAndRecord(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                repository.login(credentials).logout();
                return null;
            }
        }, repository, "Initialization");
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

    private void runAndRecord( Callable<Void> operation, Repository repository, String testName ) throws Exception {
        for (int i = 0; i < runnerConfig.repeatCount; i++) {
            LOGGER.info("run: {}", i);
            long start = System.nanoTime();
            operation.call();
            long duration = System.nanoTime() - start;
            perfData.record(repository, testName, duration);
        }
    }
}

