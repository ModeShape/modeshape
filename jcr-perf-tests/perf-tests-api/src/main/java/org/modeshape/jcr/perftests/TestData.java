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
