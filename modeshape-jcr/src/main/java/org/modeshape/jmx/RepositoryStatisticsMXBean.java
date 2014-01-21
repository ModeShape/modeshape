/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jmx;

import java.util.List;
import javax.management.MBeanException;
import javax.management.MXBean;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;

/**
 * JXM MXBean interface which exposes various monitoring information for a running repository. The information exposed by this
 * interface is obtained via the active {@link org.modeshape.jcr.api.monitor.RepositoryMonitor} instance.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@MXBean
@Details( "JMX MXBean which exposes various repository metrics" )
public interface RepositoryStatisticsMXBean {

    /**
     * Get the {@link ValueMetric} enumerations that are available for use by the caller with
     * {@link #getValues(org.modeshape.jcr.api.monitor.ValueMetric, org.modeshape.jcr.api.monitor.Window)}.
     * 
     * @return a [name, description] map of available value metrics; never null
     * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getAvailableValueMetrics()
     */
    @Details( "A list of enums which represent the available value metrics that should be used as parameters for the getValues operation" )
    List<EnumDescription> getValueMetrics();

    /**
     * Get the {@link DurationMetric} enumerations that are available for use by the caller with
     * {@link #getDurations(org.modeshape.jcr.api.monitor.DurationMetric, org.modeshape.jcr.api.monitor.Window)}.
     * 
     * @return a [name, description] map of available duration metrics; never null
     * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getAvailableDurationMetrics()
     */
    @Details( "A list of enums which represent the available duration metrics that should be used as parameters for the getDurations and getLongestRunning operations" )
    List<EnumDescription> getDurationMetrics();

    /**
     * Get the {@link Window} enumerations that are available for use by the caller with
     * {@link #getValues(org.modeshape.jcr.api.monitor.ValueMetric, org.modeshape.jcr.api.monitor.Window)} and
     * {@link #getDurations(org.modeshape.jcr.api.monitor.DurationMetric, org.modeshape.jcr.api.monitor.Window)}.
     * 
     * @return a [name, description] map of available time windows; never null
     * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getAvailableWindows()
     */
    @Details( "A list of enums which represent the available time intervals that should be used as operation parameters" )
    List<EnumDescription> getTimeWindows();

    /**
     * Get the statistics for the specified value metric during the given window in time.
     * 
     * @param metric the value metric; may not be null
     * @param windowInTime the window specifying which statistics are to be returned; may not be null
     * @return the statistical data; never null
     * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getHistory(org.modeshape.jcr.api.monitor.ValueMetric,
     *      org.modeshape.jcr.api.monitor.Window)
     * @throws javax.management.MBeanException if anything unexpected fails while performing the operation.
     */
    @Details( "Returns the values of a certain type in a given period of time" )
    public HistoricalData getValues( @Details( "The value metric enum name (see the ValueMetrics)" ) ValueMetric metric,
                                     @Details( "The time window enum name  (see the TimeWindows)" ) Window windowInTime )
        throws MBeanException;

    /**
     * Get the statics for the specified duration metric during the given window in time.
     * 
     * @param metric the duration metric; may not be null
     * @param windowInTime the window specifying which statistics are to be returned; may not be null
     * @return the statistical data; never null
     * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getHistory(org.modeshape.jcr.api.monitor.DurationMetric,
     *      org.modeshape.jcr.api.monitor.Window)
     * @throws javax.management.MBeanException if anything unexpected fails while performing the operation.
     */
    @Details( "Returns the values for a duration type in a period of time" )
    public HistoricalData getDurations( @Details( "The duration metric enum name (see the DurationMetrics)" ) DurationMetric metric,
                                        @Details( "The time window enum name (see the TimeWindows)" ) Window windowInTime )
        throws MBeanException;

    /**
     * Get the longest-running activities recorded for the specified metric. The results contain the duration records in order of
     * increasing duration, with the activity with the longest duration appearing last in the list.
     * 
     * @param metric the duration metric; may not be null
     * @return the longest duration data; never null but possibly empty if no such activities were performed
     * @see org.modeshape.jcr.api.monitor.RepositoryMonitor#getLongestRunning(org.modeshape.jcr.api.monitor.DurationMetric)
     * @throws javax.management.MBeanException if anything unexpected fails while performing the operation.
     */
    @Details( "Returns the longest running time of a duration type (e.g. longest running session)" )
    public List<DurationData> getLongestRunning( @Details( "The duration metric enum name  (see the DurationMetrics)" ) DurationMetric metric )
        throws MBeanException;
}
