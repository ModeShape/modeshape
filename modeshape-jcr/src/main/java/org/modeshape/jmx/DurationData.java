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

import java.beans.ConstructorProperties;
import java.util.Map;

/**
 * Values holder which exposes {@link org.modeshape.jcr.api.monitor.DurationActivity} to JMX.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class DurationData {

    private final long durationSeconds;
    private final Map<String, String> additionalInformation;

    /**
     * @param durationSeconds the duration value, in seconds.
     * @param additionalInformation a custom map of additional information
     */
    @ConstructorProperties( {"durationSeconds", "payload"} )
    public DurationData( long durationSeconds,
                         Map<String, String> additionalInformation ) {
        this.durationSeconds = durationSeconds;
        this.additionalInformation = additionalInformation;
    }

    /**
     * @return the duration; in seconds
     */
    public long getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * @return a Map of additional information; may be empty but never null.
     */
    public Map<String, String> getAdditionalInformation() {
        return additionalInformation;
    }
}
