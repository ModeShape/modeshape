package org.modeshape.graph.connector;

import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.observe.Observer;

public class MockRepositoryContext implements RepositoryContext {
    private final ExecutionContext context;

    public MockRepositoryContext() {
        this.context = new ExecutionContext();
    }

    public MockRepositoryContext( ExecutionContext context ) {
        this.context = context;
    }

    public Subgraph getConfiguration( int depth ) {
        return null;
    }

    public ExecutionContext getExecutionContext() {
        return context;
    }

    public Observer getObserver() {
        return null;
    }

    public RepositoryConnectionFactory getRepositoryConnectionFactory() {
        return null;
    }

}
