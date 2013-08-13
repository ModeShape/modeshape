package org.modeshape.jmx;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.api.monitor.DurationActivity;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.History;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.jcr.api.monitor.Statistics;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;

/**
 * MXBean implementation of {@link RepositoryStatisticsMXBean}.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RepositoryStatisticsBean implements RepositoryStatisticsMXBean {

    private static final Logger LOGGER = Logger.getLogger(RepositoryStatisticsBean.class);

    private final RepositoryMonitor monitor;
    private final String repositoryName;

    /**
     * @param monitor an active {@link RepositoryMonitor} instance which will be used for getting repository statistics
     * @param repositoryName a non-null String, the name of the repository.
     */
    public RepositoryStatisticsBean( RepositoryMonitor monitor,
                                     String repositoryName ) {
        this.monitor = monitor;
        this.repositoryName = repositoryName;
    }

    /**
     * Initializes & registers this MBean with the local MBean server.
     */
    public void start() {
        ObjectName beanName = null;
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            beanName = getObjectName();
            server.registerMBean(this, beanName);
        } catch (InstanceAlreadyExistsException e) {
            LOGGER.warn(JcrI18n.mBeanAlreadyRegistered, beanName);
        } catch (Exception e) {
           LOGGER.error(e, JcrI18n.cannotRegisterMBean, beanName);
        }
    }

    /**
     * Un-registers the bean from the JMX server.
     */
    public void stop() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName beanName = null;
        try {
            beanName = getObjectName();
            server.unregisterMBean(beanName);
        } catch (InstanceNotFoundException e) {
            LOGGER.warn(JcrI18n.mBeanAlreadyRegistered, beanName);
        } catch (Exception e) {
            LOGGER.error(e, JcrI18n.cannotUnRegisterMBean, beanName);
        }
    }

    private ObjectName getObjectName() throws MalformedObjectNameException {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("name", repositoryName);
        props.put("type", "RepositoryStatistics");
        return new ObjectName("org.modeshape", props);
    }

    @Override
    public List<EnumDescription> getValueMetrics() {
        List<EnumDescription> result = new ArrayList<EnumDescription>();
        for (ValueMetric metric : RepositoryMonitor.ALL_VALUE_METRICS) {
            result.add(new EnumDescription(metric.name(), metric.getDescription()));
        }
        return result;
    }

    @Override
    public List<EnumDescription> getDurationMetrics() {
        List<EnumDescription> result = new ArrayList<EnumDescription>();
        for (DurationMetric metric : RepositoryMonitor.ALL_DURATION_METRICS) {
            result.add(new EnumDescription(metric.name(), metric.getDescription()));
        }
        return result;
    }

    @Override
    public List<EnumDescription> getTimeWindows() {
        List<EnumDescription> result = new ArrayList<EnumDescription>();
        for (Window window : RepositoryMonitor.ALL_WINDOWS) {
            result.add(new EnumDescription(window.name(), window.getLiteral()));
        }
        return result;
    }

    @Override
    public HistoricalData getValues( ValueMetric metric,
                                     Window windowInTime ) throws MBeanException {
        try {
            History history = monitor.getHistory(metric, windowInTime);
            return historyToHistoricalData(history);
        } catch (RepositoryException e) {
            throw new MBeanException(e);
        }
    }

    @Override
    public HistoricalData getDurations( DurationMetric metric,
                                        Window windowInTime ) throws MBeanException {
        try {
            History history = monitor.getHistory(metric, windowInTime);
            return historyToHistoricalData(history);
        } catch (RepositoryException e) {
            throw new MBeanException(e);
        }
    }

    private HistoricalData historyToHistoricalData( History history ) {
        List<StatisticalData> statisticalData = new ArrayList<StatisticalData>();
        for (Statistics statistics : history.getStats()) {
            if (statistics != null) {
                statisticalData.add(new StatisticalData(statistics.getCount(), statistics.getMaximum(), statistics.getMinimum(),
                                                        statistics.getMean(), statistics.getVariance()));
            }
        }
        return new HistoricalData(history.getWindow().getLiteral(),
                                  history.getStartTime().getString(),
                                  history.getEndTime().getString(),
                                  statisticalData);
    }

    @Override
    public List<DurationData> getLongestRunning( DurationMetric metric ) throws MBeanException {
        List<DurationData> longestRunning = new ArrayList<DurationData>();
        try {
            for (DurationActivity durationActivity : monitor.getLongestRunning(metric)) {
                longestRunning.add(new DurationData(durationActivity.getDuration(TimeUnit.SECONDS), durationActivity.getPayload()));
            }
            return longestRunning;
        } catch (RepositoryException e) {
            throw new MBeanException(e);
        }
    }
}
