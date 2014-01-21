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
package org.modeshape.jcr;

import java.util.EnumSet;
import java.util.Set;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.monitor.DurationActivity;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.History;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;

/**
 * The session-specific monitoring interface.
 */
public class JcrRepositoryMonitor implements RepositoryMonitor {

    private final JcrSession session;
    private final boolean permitted;

    protected JcrRepositoryMonitor( JcrSession session ) throws RepositoryException {
        this.session = session;
        this.permitted = session.hasPermission("/", ModeShapePermissions.MONITOR);
    }

    protected final RepositoryStatistics statistics() {
        return session.repository().getRepositoryStatistics();
    }

    @Override
    public Set<ValueMetric> getAvailableValueMetrics() {
        return permitted ? statistics().getAvailableValueMetrics() : EnumSet.noneOf(ValueMetric.class);
    }

    @Override
    public Set<DurationMetric> getAvailableDurationMetrics() {
        return permitted ? statistics().getAvailableDurationMetrics() : EnumSet.noneOf(DurationMetric.class);
    }

    @Override
    public Set<Window> getAvailableWindows() {
        return permitted ? statistics().getAvailableWindows() : EnumSet.noneOf(Window.class);
    }

    @Override
    public History getHistory( ValueMetric metric,
                               Window windowInTime ) throws AccessDeniedException {
        if (!permitted) throw new AccessDeniedException();
        return statistics().getHistory(metric, windowInTime);
    }

    @Override
    public History getHistory( DurationMetric metric,
                               Window windowInTime ) throws AccessDeniedException {
        if (!permitted) throw new AccessDeniedException();
        return statistics().getHistory(metric, windowInTime);
    }

    @Override
    public DurationActivity[] getLongestRunning( DurationMetric metric ) throws AccessDeniedException {
        if (!permitted) throw new AccessDeniedException();
        return statistics().getLongestRunning(metric);
    }

}
