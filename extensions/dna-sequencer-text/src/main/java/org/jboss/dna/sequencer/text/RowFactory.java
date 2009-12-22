package org.jboss.dna.sequencer.text;

import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.sequencer.SequencerOutput;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.jboss.dna.graph.sequencer.StreamSequencerContext;

/**
 * A simple interface that allows an implementer to control how rows in a text file are mapped to properties (including primary
 * and mixin types) in the graph.
 * <p>
 * Implementations of this class must provide a public, no-argument constructor.
 * </p>
 * <p>
 * To use, supply the implementation class name to a {@link AbstractTextSequencer} object. New instances are created for each
 * {@link StreamSequencer#sequence(java.io.InputStream, SequencerOutput, StreamSequencerContext)}, so implementations of this
 * interface need not be thread-safe.
 * </p>
 */
@NotThreadSafe
public interface RowFactory {

    /**
     * Records a row using the provided {@link SequencerOutput} instance.
     * 
     * @param context the sequencer context
     * @param output the {@link StreamSequencer} output
     * @param columns the columns that could be parsed out for the row
     */
    void recordRow( StreamSequencerContext context,
                 SequencerOutput output,
                 String[] columns );

}
