package org.modeshape.repository.sequencer;

import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.io.GraphBatchDestination;

/**
 * The sequencer context represents the complete context of a sequencer invocation, including the execution context (which
 * contains JAAS credentials, namespace mappings, and value factories) and the I/O environment for writing output.
 * <p>
 * This class is not thread safe due to its use of {@link Destination a destination}.
 * </p>
 */
@NotThreadSafe
public class SequencerContext {

    private final ExecutionContext executionContext;
    private final Graph sourceGraph;
    private final Graph destinationGraph;
    private final Destination destination;

    public SequencerContext( ExecutionContext executionContext,
                             Graph sourceGraph,
                             Graph outputGraph ) {
        super();

        assert executionContext != null;
        assert sourceGraph != null;

        this.executionContext = executionContext;
        this.sourceGraph = sourceGraph;
        this.destinationGraph = outputGraph != null ? outputGraph : sourceGraph;
        this.destination = new GraphBatchDestination(destinationGraph.batch());
    }

    /**
     * Returns the execution context under which this sequencer context operates
     * 
     * @return the execution context under which this sequencer context operates
     */
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * Returns the I/O environment in which this sequencer context operates
     * 
     * @return the I/O environment in which this sequencer context operates
     */
    public Destination getDestination() {
        return destination;
    }

    Graph graph() {
        return this.sourceGraph;
    }

    Graph destinationGraph() {
        return destinationGraph;
    }

}
