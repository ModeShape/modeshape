package org.modeshape.jcr.api.monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 * The metrics for which ModeShape captures statistics on durations.
 * 
 * @since 3.0
 */
public enum DurationMetric {

    /**
     * The metric that captures the duration of {@link Query#execute() query executions}. Note that the payload of the
     * {@link DurationActivity} instances are the query strings.
     */
    QUERY_EXECUTION_TIME("query-execution-time", "Query duration",
                         "The metric measuring the amount of time required to execute queries."),
    /**
     * The metric that captures the duration of {@link Session sessions}. Note that the payload of the {@link DurationActivity}
     * instances are the session usern IDs.
     */
    SESSION_LIFETIME("session-lifetime", "Session duration", "The metric measuring the how long sessions are kept open."),
    /**
     * The metric that captures the duration of sequencer executions. Note that the payload of the {@link DurationActivity}
     * instances are strings containing the sequencer name and the input and output paths.
     */
    SEQUENCER_EXECUTION_TIME("sequencer-execution-time", "Sequencing duration",
                             "The metric measuring how long sequencers take to run and save the changes.");

    private static final Map<String, DurationMetric> BY_LITERAL;
    private static final Map<String, DurationMetric> BY_NAME;
    static {
        Map<String, DurationMetric> byName = new HashMap<String, DurationMetric>();
        Map<String, DurationMetric> byLiteral = new HashMap<String, DurationMetric>();
        for (DurationMetric metric : DurationMetric.values()) {
            byLiteral.put(metric.getLiteral().toLowerCase(), metric);
            byName.put(metric.name().toLowerCase(), metric);
        }
        BY_LITERAL = Collections.unmodifiableMap(byLiteral);
        BY_NAME = Collections.unmodifiableMap(byName);
    }
    private final String literal;
    private final String label;
    private final String description;

    private DurationMetric( String literal,
                            String label,
                            String description ) {
        this.literal = literal;
        this.label = label;
        this.description = description;
    }

    /**
     * The literal string form of the duration metric.
     * 
     * @return the literal string form; never null or empty
     */
    public String getLiteral() {
        return literal;
    }

    /**
     * The readable label for this metric.
     * 
     * @return the human-readable label; never null or empty
     */
    public String getLabel() {
        return label;
    }

    /**
     * Return the description for this metric.
     * 
     * @return the description; never null or empty
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the {@link DurationMetric} that has the supplied literal.
     * 
     * @param literal the literal (can be of any case); may not be null
     * @return the duration metric, or null if there is no DurationMetric enum value for the given literal string
     */
    public static DurationMetric fromLiteral( String literal ) {
        if (literal == null) return null;
        literal = literal.toLowerCase();
        DurationMetric metric = BY_LITERAL.get(literal);
        if (metric == null) BY_NAME.get(literal);
        return metric;
    }
}
