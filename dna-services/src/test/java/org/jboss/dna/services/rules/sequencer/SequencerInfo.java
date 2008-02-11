/*
 *
 */
package org.jboss.dna.services.rules.sequencer;

/**
 * @author John Verhaeg
 */
public final class SequencerInfo {

    String name;

    public SequencerInfo( String name ) {
        this.name = name;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }
}
