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

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The immutable representation of activities with measureable durations.
 * 
 * @since 3.0
 */
public interface DurationActivity extends Comparable<DurationActivity> {

    /**
     * Get the duration of this activity.
     * 
     * @param unit the desired time unit for the duration
     * @return the duration in the specified time unit
     */
    public long getDuration( TimeUnit unit );

    /**
     * Get the payload for this activity.
     * 
     * @return the payload; may be null
     */
    public Map<String, String> getPayload();
}
