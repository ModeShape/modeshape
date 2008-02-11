/*
 *
 */
package org.jboss.dna.services.sequencers;

import java.util.List;
import javax.jcr.Node;
import org.jboss.dna.common.util.Logger;

/**
 * @author Randall Hauch
 */
public interface ISequencerProvider {

    List<ISequencer> getSequencersToProcess( Node node, Logger logger );

    ISequencerConfig getConfigurationFor( ISequencer sequencer );

}
