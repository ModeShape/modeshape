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
 * Class which holds the performance statistics for the operations ran against repositories by <code>PerformanceTestSuiteRunner</code>
 *
 * @author Horia Chiorean
 */
public final class PerformanceData {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTestSuiteRunner.class);

    //map [repository name -> [operation name, [operation duration 1(ms), operation duration 2(ms)]]
    private Map<String, Map<String, List<Long>>> reposPerformanceMap = new HashMap<String, Map<String, List<Long>>>();
    private TimeUnit duration = TimeUnit.MILLISECONDS;

    void record( Repository repository, String operationName, long duration ) {
        duration = this.duration.convert(duration, TimeUnit.NANOSECONDS);
        String repoName = repository.getClass().getSimpleName();
        LOGGER.info("Recording statistic: repo {} operation {} duration(ms) {}", new Object[] {repoName, operationName, duration});

        if (!reposPerformanceMap.containsKey(repoName)) {
            reposPerformanceMap.put(repoName, new HashMap<String, List<Long>>());
        }
        Map<String, List<Long>> repoPerformanceMap = reposPerformanceMap.get(repoName);
        List<Long> existingDataForOperation = repoPerformanceMap.get(operationName);
        if (existingDataForOperation != null) {
            existingDataForOperation.add(duration);
        } else {
            existingDataForOperation = new ArrayList<Long>();
            existingDataForOperation.add(duration);
            repoPerformanceMap.put(operationName, existingDataForOperation);
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
        Map<String, List<Long>> operationsMap = reposPerformanceMap.get(repoName);
        for (String operationName : operationsMap.keySet()) {
            double[] fiveNrSummary = StatisticsCalculator.calculate5NrSummary(operationsMap.get(operationName));
            ps.printf(repoName + "#" + operationName +" [%.0f; %.2f; %.2f; %.2f; %.0f]%n", fiveNrSummary[0],
                    fiveNrSummary[1], fiveNrSummary[2], fiveNrSummary[3], fiveNrSummary[4]);
        }
    }

    private void printHeader( PrintStream ps ) {
        ps.printf("Operation(%s) [Minimum, 1st Quartile, Median, 3rd Quartile, Maximum]%n", duration.toString());
        ps.println("-----------------------------------------------------------------------");
    }
}
