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
     * An empty array.
     */
    DurationActivity[] NO_DURATION_RECORDS = new DurationActivity[0];

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
