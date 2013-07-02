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
package org.modeshape.jboss.metric;

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.History;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.jcr.api.monitor.Window;

/**
 * Represents a ModeShape duration metric operation.
 */
final class GetDurationMetric extends ModeShapeMetricHandler {

    /**
     * The ModeShape metric (never <code>null</code>)
     */
    private final DurationMetric metric;

    /**
     * @param metric the duration metric whose operation is being constructed (cannot be <code>null</code>)
     * @param window the metric window (cannot be <code>null</code>)
     */
    public GetDurationMetric( final DurationMetric metric,
                              final Window window ) {
        super(window);

        CheckArg.isNotNull(metric, "metric");
        this.metric = metric;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.modeshape.jboss.metric.ModeShapeMetricHandler#history(org.modeshape.jcr.api.monitor.RepositoryMonitor)
     */
    @Override
    protected History history( final RepositoryMonitor repoStats ) throws Exception {
        CheckArg.isNotNull(repoStats, "repoStats");
        return repoStats.getHistory(this.metric, window());
    }

}
