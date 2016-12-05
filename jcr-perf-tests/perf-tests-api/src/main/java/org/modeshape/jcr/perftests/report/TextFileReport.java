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
package org.modeshape.jcr.perftests.report;

import org.modeshape.jcr.perftests.StatisticalData;
import org.modeshape.jcr.perftests.TestData;
import org.modeshape.jcr.perftests.util.DurationsConverter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Report generator which will output to a text file various statistical data about the test which have been run.
 *
 * @author Horia Chiorean
 */
public final class TextFileReport extends TestReportGenerator {

    private static final String DEFAULT_REPORT_FILENAME = "perf-report.txt";

    public TextFileReport(  ) {
        super(DEFAULT_REPORT_FILENAME, TimeUnit.SECONDS);
    }

    @Override
    public void generateReport( TestData testData ) throws Exception {
        File reportFile = getReportFile();
        PrintStream ps = new PrintStream(new FileOutputStream(reportFile, true));
        try {
            printHeader(ps);
            printStatisticalData(testData, ps);
            printFailures(testData, ps);
        } finally {
            ps.close();
        }
    }

    private void printHeader( PrintStream ps ) {
        ps.println();
        ps.println("Date: " + SimpleDateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date()));
        ps.printf("Test [Minimum, 1st Quartile, Median, 3rd Quartile, Maximum, Standard Deviation] %s %n", timeUnit);
        ps.println("-----------------------------------------------------------------------");
    }

    private void printFailures( TestData testData, PrintStream ps ) {
        Set<String> failedTestNames = testData.getFailedTestNames();
        if (failedTestNames.isEmpty()) {
            return;
        }
        ps.println("-----------------------------------------------------------------------");
        ps.println("Failures count:" + failedTestNames.size());
        ps.println(failedTestNames.toString());
        ps.println("See the log file for more information");
    }

    private void printStatisticalData( TestData testData, PrintStream ps ) {
        Set<String> testNames = testData.getSuccessfulTestNames();

        for (String testName : testNames) {
            List<Double> convertedDurations = DurationsConverter.convertFromNanos(testData.getTestDurationsNanos(testName), timeUnit);
            StatisticalData statisticalData = new StatisticalData(convertedDurations.toArray(new Double[convertedDurations.size()]));

            double[] fiveNrSummary = statisticalData.fiveNumberSummary();
            ps.printf(testName + "(%d runs) [%.4f; %.4f; %.4f; %.4f; %.4f; %.4f]%n", convertedDurations.size(),
                    fiveNrSummary[0], fiveNrSummary[1], fiveNrSummary[2], fiveNrSummary[3], fiveNrSummary[4], statisticalData.standardDeviation());
        }
    }
}
