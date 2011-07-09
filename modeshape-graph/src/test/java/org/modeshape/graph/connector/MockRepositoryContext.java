package org.modeshape.graph.connector;

import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.observe.Observer;

public class MockRepositoryContext implements RepositoryContext {
    private final ExecutionContext context;
    private final RepositorySource source;

    public MockRepositoryContext() {
        this(new ExecutionContext(), null);
    }

    public MockRepositoryContext( ExecutionContext context ) {
        this(context, null);
    }

    public MockRepositoryContext( ExecutionContext context,
                                  RepositorySource source ) {
        this.context = context;
        this.source = source;
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
        if (source == null) return null;

        return new RepositoryConnectionFactory() {

            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return source.getConnection();
            }

        };
    }

}
