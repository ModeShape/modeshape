package org.modeshape.jcr.perftests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Holder for the configuration options of a <code>PerformanceTestSuiteRunner</code>. Normally this class holds the values
 * read from a configuration file. (e.g. runner.properties)
 *
 * @author Horia Chiorean
 */
public final class RunnerConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunnerConfiguration.class);
    //default config file, loaded from classpath
    private static final String DEFAULT_CONFIG_FILE = "runner.properties";

    List<String> excludeTestsRegExp = new ArrayList<String>();
    List<String> includeTestsRegExp = new ArrayList<String>();
    List<String> scanSubPackages = new ArrayList<String>();
    int repeatCount = 1;

    RunnerConfiguration( String fileName ) {
        try {
            Properties configParams = new Properties();
            configParams.load(getClass().getClassLoader().getResourceAsStream(fileName));
            initRunner(configParams);
        } catch (IOException e) {
            LOGGER.warn("Cannot load config file. Will use defaults ", e);
        }
    }

    /**
     * Creates a new instance by reading the {@link RunnerConfiguration#DEFAULT_CONFIG_FILE}
     */
    public RunnerConfiguration() {
        this(DEFAULT_CONFIG_FILE);
    }

    /**
     * Adds regexp patterns to exclude from running
     */
    public RunnerConfiguration addExcludeTests( String... excludeTestsRegExp ) {
        this.excludeTestsRegExp.addAll(Arrays.asList(excludeTestsRegExp));
        return this;
    }

    public RunnerConfiguration addIncludeTests( String... includeTestsRegExp ) {
        this.includeTestsRegExp.addAll(Arrays.asList(includeTestsRegExp));
        return this;
    }

    public RunnerConfiguration addScanSubPackages( String... scanSubPackages ) {
        this.scanSubPackages.addAll(Arrays.asList(scanSubPackages));
        return this;
    }

    public RunnerConfiguration withRepeatCount( int repeatCount ) {
        this.repeatCount = repeatCount;
        return this;
    }

    private void initRunner( Properties configParams ) {
        parseMultiValuedString(configParams.getProperty("tests.exclude"), excludeTestsRegExp);
        parseMultiValuedString(configParams.getProperty("tests.include"), includeTestsRegExp);
        parseMultiValuedString(configParams.getProperty("scan.subPackages"), scanSubPackages);
        repeatCount = Integer.valueOf(configParams.getProperty("repeat.count"));
    }

    private void parseMultiValuedString( String multiValueString, List<String> collector ) {
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
}
