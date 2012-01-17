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
package org.modeshape.jcr.api.monitor;

import java.util.concurrent.TimeUnit;
import org.modeshape.jcr.api.value.DateTime;

/**
 * An immutable history of a metric for a given window in time.
 * 
 * @since 3.0
 */
public interface History {

    /**
     * Get the kind of window.
     * 
     * @return the window type; never null
     */
    public Window getWindow();

    /**
     * Get the total duration of this history window.
     * 
     * @param unit the desired time unit; if null, then {@link TimeUnit#SECONDS} is used
     * @return the duration
     */
    public long getTotalDuration( TimeUnit unit );

    /**
     * Get the timestamp (including time zone information) at which this history window starts.
     * 
     * @return the time at which this window starts
     */
    public DateTime getStartTime();

    /**
     * Get the timestamp (including time zone information) at which this history window ends.
     * 
     * @return the time at which this window ends
     */
    public DateTime getEndTime();

    /**
     * Get the statistics for that make up the history.
     * 
     * @return the statistics; never null, but the array may contain null if the window is longer than the lifetime of the
     *         repository
     */
    Statistics[] getStats();
}
