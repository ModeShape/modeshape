package org.modeshape.jcr.api.monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The specification of the window for obtaining the history and statistics for a given metric.
 * 
 * @since 3.0
 */
public enum Window {
    /**
     * The window for accessing a metric's history and statistics for ten 5-second intervals during the last minute (60 seconds).
     */
    PREVIOUS_60_SECONDS("previous-60-seconds"),
    /**
     * The window for accessing a metric's history and statistics for each minute during the last hour (60 minutes).
     */
    PREVIOUS_60_MINUTES("previous-60-minutes"),
    /**
     * The window for accessing a metric's history and statistics for each hour during the last day (24 hours).
     */
    PREVIOUS_24_HOURS("previous-24-hours"),
    /**
     * The window for accessing a metric's history and statistics for each day during the last week (7 days).
     */
    PREVIOUS_7_DAYS("previous-7-days"),
    /**
     * The window for accessing a metric's history and statistics for each week during the last year (52 weeks).
     */
    PREVIOUS_52_WEEKS("previous-52-wees");

    private static final Map<String, Window> BY_LITERAL;
    private static final Map<String, Window> BY_NAME;
    static {
        Map<String, Window> byLiteral = new HashMap<String, Window>();
        Map<String, Window> byName = new HashMap<String, Window>();
        for (Window window : Window.values()) {
            byLiteral.put(window.getLiteral().toLowerCase(), window);
            byName.put(window.name().toLowerCase(), window);
        }
        BY_LITERAL = Collections.unmodifiableMap(byLiteral);
        BY_NAME = Collections.unmodifiableMap(byName);
    }
    private final String literal;

    private Window( String literal ) {
        this.literal = literal;
    }

    /**
     * The literal string form of the window.
     * 
     * @return the literal string form; never null or empty
     */
    public String getLiteral() {
        return literal;
    }

    /**
     * Get the {@link Window} that has the supplied literal.
     * 
     * @param literal the literal (can be of any case); may not be null
     * @return the window, or null if there is no Window enum value for the given literal string
     */
    public static Window fromLiteral( String literal ) {
        if (literal == null) return null;
        literal = literal.toLowerCase();
        Window window = BY_LITERAL.get(literal);
        if (window == null) BY_NAME.get(literal);
        return window;
    }
}
