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
