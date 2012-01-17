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
