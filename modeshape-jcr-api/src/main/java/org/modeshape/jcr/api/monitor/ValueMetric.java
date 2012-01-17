package org.modeshape.jcr.api.monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.Lock;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.version.VersionManager;

/**
 * The metrics for which ModeShape captures statistics on running values.
 * 
 * @since 3.0
 */
public enum ValueMetric {
    /**
     * The metric that captures the number of {@link Session sessions} that are open during the window.
     */
    SESSION_COUNT("session-count", true, "Active sessions", "The number of sessions that are active during the window."),
    /**
     * The metric that captures the number of {@link Query queries} that are executing during the window.
     */
    QUERY_COUNT("query-count", true, "Active queries", "The number of queries that are executing during the window"),
    /**
     * The metric that records the number of {@link Workspace workspaces} in existance during the window.
     */
    WORKSPACE_COUNT("workspace-count", true, "Workspace count", "The number of workspaces that exist during the window"),
    /**
     * The metric that records the number of {@link EventListener observation listeners} in existance during the window.
     */
    LISTENER_COUNT("listener-count", true, "Active listeners", "The number of listeners registered during the window."),
    /**
     * The metric that records the number of {@link Event observation events} that are in the queue and awaiting processing (and
     * still have yet to be sent to the liseners).
     */
    EVENT_QUEUE_SIZE("event-queue-size", true, "Event queue size",
                     "The number of events at the end of the window that have yet to be processed and sent to listeners"),
    /**
     * The metric that records the number of {@link Event observation events} that have been sent to at least one listener.
     */
    EVENT_COUNT("event-count", false, "Event count",
                "The number of events that have been sent to at least one listener during the window."),
    /**
     * The metric that records the number of {@link Lock#isSessionScoped() session-scoped} {@link Lock JCR locks} in existence
     * during the window.
     */
    SESSION_SCOPED_LOCK_COUNT(
                              "session-scoped-lock-count",
                              true,
                              "Session-scoped locks",
                              "The number of session-scoped locks that were held by clients during the window. The values go up or down from one window to the next as clients lock and unlock nodes."),
    /**
     * The metric that records the number of {@link Lock#isSessionScoped() non-session-scoped} {@link Lock JCR locks} in existance
     * during the window.
     */
    OPEN_SCOPED_LOCK_COUNT(
                           "open-scoped-lock-count",
                           true,
                           "Open-scoped locks",
                           "The number of locks that were held by clients during the window. The values go up or down from one window to the next as clients lock and unlock nodes."),
    /**
     * The metric that captures the number of {@link Session#save()} calls that have occurred during the window.
     */
    SESSION_SAVES("session-saves", false, "Saves", "The number of save operations called on sessions during the window."),
    /**
     * The metric that captures the number of nodes that were created, updated, or deleted during the window as part of the
     * {@link Session#save()}, {@link VersionManager#checkin(String)},
     * {@link Workspace#importXML(String, java.io.InputStream, int) Workspace.import} and other calls that change content.
     */
    NODE_CHANGES(
                 "node-changes",
                 false,
                 "Changed nodes",
                 "The number of nodes that were created, updated or deleted during the window due to session saves, workspace imports, or version checkin calls."),
    /**
     * The metric that records the number of nodes that were sequenced.
     */
    SEQUENCER_QUEUE_SIZE("sequenced-queue-size", true, "Sequencer queue size",
                         "The number of nodes at the end of the window that have yet to be sequenced."),
    /**
     * The metric that records the number of nodes that were sequenced.
     */
    SEQUENCED_COUNT("sequenced-count", false, "Sequenced nodes", "The number of nodes that were sequenced during the window.");

    private static final Map<String, ValueMetric> BY_LITERAL;
    private static final Map<String, ValueMetric> BY_NAME;
    static {
        Map<String, ValueMetric> byName = new HashMap<String, ValueMetric>();
        Map<String, ValueMetric> byLiteral = new HashMap<String, ValueMetric>();
        for (ValueMetric metric : ValueMetric.values()) {
            byLiteral.put(metric.getLiteral().toLowerCase(), metric);
            byName.put(metric.name().toLowerCase(), metric);
        }
        BY_LITERAL = Collections.unmodifiableMap(byLiteral);
        BY_NAME = Collections.unmodifiableMap(byName);
    }
    private final String literal;
    private final String label;
    private final String description;
    private final boolean continuous;

    private ValueMetric( String literal,
                         boolean continuous,
                         String label,
                         String description ) {
        this.literal = literal;
        this.label = label;
        this.description = description;
        this.continuous = continuous;
    }

    /**
     * The literal string form of the value metric.
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
     * Return whether this metric's values are continuous across multiple windows. In other words, the values of a continuous
     * metric will increase <i>and</i> decrease over time.
     * <p>
     * On the other hand, the values of a non-continous metric will have values that always start at zero at the beginning of a
     * window.
     * </p>
     * 
     * @return continuous
     */
    public boolean isContinuous() {
        return continuous;
    }

    /**
     * Get the {@link ValueMetric} that has the supplied literal.
     * 
     * @param literal the literal (can be of any case); may not be null
     * @return the value metric, or null if there is no ValueMetric enum value for the given literal string
     */
    public static ValueMetric fromLiteral( String literal ) {
        if (literal == null) return null;
        literal = literal.toLowerCase();
        ValueMetric metric = BY_LITERAL.get(literal);
        if (metric == null) BY_NAME.get(literal);
        return metric;
    }
}
