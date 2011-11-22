package org.modeshape.jcr.perftests;

import javax.jcr.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Class which holds the performance statistics for the operations ran agains repositories by <code>PerformanceTestSuiteRunner</code>
 *
 * @author Horia Chiorean
 */
public final class PerformanceStatistics {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceTestSuiteRunner.class);

    //map [repository name -> [operation name, [operation duration 1(ms), operation duration 2(ms)]]
    private Map<String, Map<String, List<Long>>> reposPerformanceMap = new HashMap<String, Map<String, List<Long>>>();

    void recordStatisticForRepository(Repository repository, String operationName, long duration) {
        duration = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
        String repoName = repository.getClass().getSimpleName();
        LOGGER.info("Recording statistic: repo {} operation {} duration(ms) {}", new Object[] {repoName, operationName, duration});

        if (!reposPerformanceMap.containsKey(repoName)) {
            reposPerformanceMap.put(repoName, new HashMap<String, List<Long>>());
        }
        Map<String, List<Long>> repoPerformanceMap = reposPerformanceMap.get(repoName);
        List<Long> existingDataForOperation = repoPerformanceMap.get(operationName);
        if (existingDataForOperation != null) {
            existingDataForOperation.add(duration);
        }
        else {
            existingDataForOperation = new ArrayList<Long>();
            existingDataForOperation.add(duration);
            repoPerformanceMap.put(operationName, existingDataForOperation);
        }
    }
}
