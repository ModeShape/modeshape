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
    private final Map<String,String> additionalInformation;

    /**
     * @param durationSeconds the duration value, in seconds.
     * @param additionalInformation a custom map of additional information
     */
    @ConstructorProperties({"durationSeconds", "payload"})
    public DurationData( long durationSeconds,
                         Map<String,String> additionalInformation ) {
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
