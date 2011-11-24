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
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Class which holds the performance statistics for the test ran against repositories by <code>PerformanceTestSuiteRunner</code>
 *
 * @author Horia Chiorean
 */
final class TestData {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestData.class);
    public static final String PERF_REPORT_FILENAME = "perf-report.txt";

    /** Map [test name, [test duration 1(ms), test duration 2(ms)]] */
    private Map<String, List<Long>> durationsMap = new TreeMap<String, List<Long>>();

    /** List of the names of the operations that have failed */
    private Set<String> failedOperations = new TreeSet<String>();

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
        failedOperations.add(operationName);
        LOGGER.warn(operationName + " failure", cause);
    }

    void print5NrSummary( TimeUnit timeUnit ) throws Exception {
        File reportDir = new File(getClass().getClassLoader().getResource(".").toURI());
        if (!reportDir.exists() || !reportDir.isDirectory()) {
            throw new IllegalStateException("Cannot locate target folder for performance report");
        }
        File reportFile = new File(reportDir, PERF_REPORT_FILENAME);
        PrintStream ps = new PrintStream(new FileOutputStream(reportFile, true));
        printHeader(ps, timeUnit);

        try {
            print5NrSummaryForRepo(ps, timeUnit);
            printFailures(ps);
        } finally {
            ps.close();
        }
    }

    private void printFailures( PrintStream ps ) {
        if (failedOperations.isEmpty()) {
            return;
        }
        ps.println("-----------------------------------------------------------------------");
        ps.println("Failures count:" + failedOperations.size());
        ps.println(failedOperations.toString());
        ps.println("See the log file for more information");
    }

    private void print5NrSummaryForRepo( PrintStream ps, TimeUnit timeUnit ) {
        for (String testName : durationsMap.keySet()) {
            List<Long> convertedDurations = convertDurations(timeUnit, testName);
            double[] fiveNrSummary = StatisticsCalculator.calculate5NumberSummary(convertedDurations);
            ps.printf(testName + " [%.0f; %.2f; %.2f; %.2f; %.0f]%n", fiveNrSummary[0],
                    fiveNrSummary[1], fiveNrSummary[2], fiveNrSummary[3], fiveNrSummary[4]);
        }
    }

    private List<Long> convertDurations( TimeUnit duration, String testName ) {
        List<Long> durationsNanos = durationsMap.get(testName);
        List<Long> convertedDurations = new ArrayList<Long>(durationsNanos.size());
        for (long durationNano : durationsNanos) {
            convertedDurations.add(duration.convert(durationNano, TimeUnit.NANOSECONDS));
        }
        return convertedDurations;
    }

    private void printHeader( PrintStream ps, TimeUnit timeUnit ) {
        ps.println();
        ps.println("Date: " + SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date()));
        ps.printf("Test [Minimum, 1st Quartile, Median, 3rd Quartile, Maximum] %s %n", timeUnit);
        ps.println("-----------------------------------------------------------------------");
    }
}
