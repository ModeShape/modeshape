/*
 *
 */
package org.jboss.dna.engine.sequencers;

import java.util.List;
import javax.jcr.Node;
import org.jboss.dna.common.util.Logger;

/**
 * @author Randall Hauch
 */
public class DroolsSequencerProvider implements ISequencerProvider {

    /**
     * {@inheritDoc}
     */
    public List<ISequencer> getSequencersToProcess( Node node, Logger logger ) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ISequencerConfig getConfigurationFor( ISequencer sequencer ) {
        return null;
    }

}
