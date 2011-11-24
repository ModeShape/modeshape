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

    /** default config file, loaded from classpath */
    private static final String DEFAULT_CONFIG_FILE = "runner.properties";

    final List<String> excludeTestsRegExp = new ArrayList<String>();
    final List<String> includeTestsRegExp = new ArrayList<String>();
    final List<String> scanSubPackages = new ArrayList<String>();

    int repeatCount = 1;
    int warmupCount = 1;

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
    public RunnerConfiguration addTestsToExclude( String... excludeTestsRegExp ) {
        this.excludeTestsRegExp.addAll(Arrays.asList(excludeTestsRegExp));
        return this;
    }

    public RunnerConfiguration addTestsToInclude( String... includeTestsRegExp ) {
        this.includeTestsRegExp.addAll(Arrays.asList(includeTestsRegExp));
        return this;
    }

    public RunnerConfiguration addSubPackagesToScan( String... scanSubPackages ) {
        this.scanSubPackages.addAll(Arrays.asList(scanSubPackages));
        return this;
    }

    public RunnerConfiguration setRepeatCount( int repeatCount ) {
        this.repeatCount = repeatCount;
        return this;
    }

    public RunnerConfiguration setWarmupCount( int warmupCount ) {
        this.warmupCount = warmupCount;
        return this;
    }

    private void initRunner( Properties configParams ) {
        parseMultiValuedString(configParams.getProperty("tests.exclude"), excludeTestsRegExp);
        parseMultiValuedString(configParams.getProperty("tests.include"), includeTestsRegExp);
        parseMultiValuedString(configParams.getProperty("scan.subPackages"), scanSubPackages);
        repeatCount = Integer.valueOf(configParams.getProperty("repeat.count"));
        warmupCount = Integer.valueOf(configParams.getProperty("warmup.count"));
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
