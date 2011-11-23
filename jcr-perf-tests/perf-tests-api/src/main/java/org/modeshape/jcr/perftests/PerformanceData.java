package org.modeshape.jcr.perftests;

import javax.jcr.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Class which holds the performance statistics for the test ran against repositories by <code>PerformanceTestSuiteRunner</code>
 *
 * @author Horia Chiorean
 */
final class PerformanceData {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTestSuiteRunner.class);

    //map [repository name -> [test name, [test duration 1(ms), test duration 2(ms)]]
    private Map<String, Map<String, List<Long>>> reposPerformanceMap = new TreeMap<String, Map<String, List<Long>>>();
    private TimeUnit duration;

    PerformanceData( TimeUnit duration ) {
        this.duration = duration;
    }

    void record( Repository repository, String testName, long durationNanos ) {
        long convertedDuration = this.duration.convert(durationNanos, TimeUnit.NANOSECONDS);
        String repoName = repository.getClass().getSimpleName();
        LOGGER.info("Recording statistic: repo {} test {} duration(ms) {}", new Object[] {repoName, testName, convertedDuration});

        if (!reposPerformanceMap.containsKey(repoName)) {
            reposPerformanceMap.put(repoName, new TreeMap<String, List<Long>>());
        }
        Map<String, List<Long>> repoPerformanceMap = reposPerformanceMap.get(repoName);
        List<Long> testData = repoPerformanceMap.get(testName);
        if (testData != null) {
            testData.add(convertedDuration);
        } else {
            testData = new ArrayList<Long>();
            testData.add(convertedDuration);
            repoPerformanceMap.put(testName, testData);
        }
    }

    void print5NrSummary() throws Exception {
        File reportDir = new File(getClass().getClassLoader().getResource(".").toURI());
        if (!reportDir.exists() || !reportDir.isDirectory()) {
            throw new IllegalStateException("Cannot locate target folder for performance report");
        }
        File reportFile = new File(reportDir, "perf_report.txt");
        PrintStream ps;

        if (reportFile.exists()) {
            ps = new PrintStream(new FileOutputStream(reportFile, true));
        } else {
            ps = new PrintStream(new FileOutputStream(reportFile, false));
            printHeader(ps);
        }

        try {
            for (String repoName : reposPerformanceMap.keySet()) {
                print5NrSummaryForRepo(ps, repoName);
            }
        } finally {
            ps.close();
        }
    }

    private void print5NrSummaryForRepo( PrintStream ps, String repoName ) {
        Map<String, List<Long>> testData = reposPerformanceMap.get(repoName);
        for (String testName : testData.keySet()) {
            double[] fiveNrSummary = StatisticsCalculator.calculate5NrSummary(testData.get(testName));
            ps.printf(repoName + "#" + testName +" [%.0f; %.2f; %.2f; %.2f; %.0f]%n", fiveNrSummary[0],
                    fiveNrSummary[1], fiveNrSummary[2], fiveNrSummary[3], fiveNrSummary[4]);
        }
    }

    private void printHeader( PrintStream ps ) {
        ps.printf("Test(%s) [Minimum, 1st Quartile, Median, 3rd Quartile, Maximum]%n", duration.toString());
        ps.println("-----------------------------------------------------------------------");
    }
}
