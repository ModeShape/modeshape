package org.modeshape.jcr.api.sequencer;

import java.util.Calendar;

/**
 * The sequencer context represents the complete context of a sequencer invocation. Currently, this information includes the name
 * of the repository, and the timestamp that represents the current time of execution.
 */
public interface SequencerContext {

    /**
     * Get the name of the repository.
     * 
     * @return the repository name; never null
     */
    String getRepositoryName();

    /**
     * Get the timestamp of the sequencing. This is always the timestamp of the change event that is being processed.
     * 
     * @return timestamp the "current" timestamp; never null
     */
    Calendar getTimestamp();
}
