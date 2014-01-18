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

import java.lang.management.ManagementFactory;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import org.junit.Test;
import org.modeshape.jcr.SingleUseAbstractTest;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for {@link RepositoryStatisticsMXBean}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RepositoryStatisticsMXBeanTest extends SingleUseAbstractTest {

    private static final MBeanServer SERVER = ManagementFactory.getPlatformMBeanServer();

    private ObjectName mBeanName;

    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        mBeanName = new ObjectName("org.modeshape:type=RepositoryStatistics,name=" + REPO_NAME);
    }

    @Test
    public void shouldRetrieveMBeanInfo() throws Exception {
        MBeanInfo mBeanInfo = SERVER.getMBeanInfo(mBeanName);

        assertNotNull(mBeanInfo);
        assertEquals(3, mBeanInfo.getAttributes().length);
        assertEquals(3, mBeanInfo.getOperations().length);
    }

    @Test
    public void shouldRetrieveAttributes() throws Exception {
        CompositeData[] valueMetrics =  (CompositeData[])SERVER.getAttribute(mBeanName, "ValueMetrics");
        assertEquals(valueMetrics.length, RepositoryMonitor.ALL_VALUE_METRICS.size());

        CompositeData[] durationMetrics =  (CompositeData[])SERVER.getAttribute(mBeanName, "DurationMetrics");
        assertEquals(durationMetrics.length, RepositoryMonitor.ALL_DURATION_METRICS.size());

        CompositeData[] windows =  (CompositeData[])SERVER.getAttribute(mBeanName, "TimeWindows");
        assertEquals(windows.length, RepositoryMonitor.ALL_WINDOWS.size());
    }

    @Test
    public void shouldRetrieveValueMetrics() throws Exception {
        Object[] arguments = new Object[] { ValueMetric.SESSION_COUNT.name(), Window.PREVIOUS_60_SECONDS.name()};
        String[] signature = new String[] {String.class.getName(), String.class.getName()};
        Object result = SERVER.invoke(mBeanName, "getValues", arguments, signature);
        assertHistoricalData(result);
    }

    @Test
    public void shouldRetrieveDurationMetrics() throws Exception {
        Object[] arguments = new Object[] { DurationMetric.SESSION_LIFETIME.name(), Window.PREVIOUS_60_SECONDS.name()};
        String[] signature = new String[] {String.class.getName(), String.class.getName()};
        Object result = SERVER.invoke(mBeanName, "getDurations", arguments, signature);
        assertHistoricalData(result);
    }

    @Test
    public void shouldRetrieveLongestRunningDuration() throws Exception {
        Object[] arguments = new Object[] { DurationMetric.SESSION_LIFETIME.name()};
        String[] signature = new String[] {String.class.getName()};
        Object result = SERVER.invoke(mBeanName, "getLongestRunning", arguments, signature);
        assertNotNull(result);
        assertTrue(result instanceof CompositeData[]);
    }

    private void assertHistoricalData( Object result ) {
        assertNotNull(result);
        CompositeData data = (CompositeData) result;
        assertTrue(data.containsKey("timeWindow"));
        assertTrue(data.containsKey("start"));
        assertTrue(data.containsKey("end"));
        assertTrue(data.containsKey("statisticalData"));
    }
}
