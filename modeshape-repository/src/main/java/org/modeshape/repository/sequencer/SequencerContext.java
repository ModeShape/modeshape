package org.modeshape.repository.sequencer;

import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.io.GraphBatchDestination;
import org.modeshape.graph.property.DateTime;

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
    private final DateTime timestamp;

    public SequencerContext( ExecutionContext executionContext,
                             Graph sourceGraph,
                             Graph outputGraph,
                             DateTime timestamp ) {
        super();

        assert executionContext != null;
        assert sourceGraph != null;
        assert timestamp != null;

        this.executionContext = executionContext;
        this.sourceGraph = sourceGraph;
        this.destinationGraph = outputGraph != null ? outputGraph : sourceGraph;
        this.destination = new GraphBatchDestination(destinationGraph.batch());
        this.timestamp = timestamp;
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
     * Get the timestamp of the sequencing. This is always the timestamp of the change event that is being processed.
     * 
     * @return timestamp the "current" timestamp; never null
     */
    public DateTime getTimestamp() {
        return timestamp;
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
