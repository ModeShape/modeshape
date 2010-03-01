package org.modeshape.sequencer.java.metadata;

import java.util.LinkedList;
import java.util.List;

/**
 * Metadata for Java enums.
 *
 */
public class EnumMetadata extends ClassMetadata {

    private final List<String> values = new LinkedList<String>();

    public List<String> getValues() {
        return values;
    }
    
}

