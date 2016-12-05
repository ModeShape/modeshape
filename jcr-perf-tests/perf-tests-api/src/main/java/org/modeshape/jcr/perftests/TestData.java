/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.perftests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Class which holds the performance statistics for the test ran against repositories by <code>PerformanceTestSuiteRunner</code>
 *
 * @author Horia Chiorean
 */
public final class TestData {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestData.class);

    /** Map [test name, [test duration 1(ms), test duration 2(ms)]] */
    private Map<String, List<Long>> durationsMap = new TreeMap<String, List<Long>>();

    /** List of the names of the operations that have failed */
    private Set<String> failedTests = new TreeSet<String>();

    void recordSuccess( String operationName, long durationNanos ) {
        LOGGER.info("{} : {} (ns)", new Object[] {operationName, durationNanos});

        List<Long> testData = durationsMap.get(operationName);
        if (testData != null) {
            testData.add(durationNanos);
        } else {
            testData = new ArrayList<Long>();
            testData.add(durationNanos);
            durationsMap.put(operationName, testData);
        }
    }

    void recordFailure( String operationName, Throwable cause ) {
        failedTests.add(operationName);
        LOGGER.warn(operationName + " failure", cause);
    }

    public Set<String> getSuccessfulTestNames() {
        return Collections.unmodifiableSet(durationsMap.keySet());
    }

    public Set<String> getFailedTestNames() {
        return Collections.unmodifiableSet(failedTests);
    }

    public List<Long> getTestDurationsNanos(String testName) {
        if (!durationsMap.containsKey(testName)) {
            Collections.emptyList();
        }
        return Collections.unmodifiableList(durationsMap.get(testName));
    }
}
