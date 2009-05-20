package org.jboss.dna.repository.sequencer;

import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.io.Destination;
import org.jboss.dna.graph.io.GraphBatchDestination;

/**
 * The sequencer context represents the complete context of a sequencer invocation, including the execution context
 * (which contains JAAS credentials, namespace mappings, and value factories) and the I/O environment for writing
 *  output.
 * 
 *  <p>
 *  This class is not thread safe due to its use of {@link Destination a destination}.
 *  </p>
 */
@NotThreadSafe
public class SequencerContext {
    
    private final ExecutionContext executionContext;
    private final Graph graph;
    private final Destination destination;
    
    public SequencerContext( ExecutionContext executionContext,
                             Graph graph ) {
        super();
        
        assert executionContext != null;
        assert graph != null;
        
        this.executionContext = executionContext;
        this.graph = graph;
        this.destination = new GraphBatchDestination(graph.batch());
    }

    /**
     * Returns the execution context under which this sequencer context operates
     * @return the execution context under which this sequencer context operates
     */
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * Returns the I/O environment in which this sequencer context operates
     * @return the I/O environment in which this sequencer context operates
     */
    public Destination getDestination() {
        return destination;
    }
    
    Graph graph() {
        return this.graph;
    }

}
