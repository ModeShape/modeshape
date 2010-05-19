package org.modeshape.graph.connector.base;

import java.util.HashMap;
import java.util.Map;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.InvalidWorkspaceException;

public class MockPathRepository extends Repository<MockPathNode, MockPathWorkspace> {

    protected final Map<String, MockPathWorkspace> workspaces = new HashMap<String, MockPathWorkspace>();

    public MockPathRepository( BaseRepositorySource source ) {
        super(source);
        initialize();
    }

    @Override
    public PathTransaction<MockPathNode, MockPathWorkspace> startTransaction( ExecutionContext context,
                                                                              boolean readonly ) {
        return new MockPathTransaction(this);
    }

    public class MockPathTransaction extends PathTransaction<MockPathNode, MockPathWorkspace> {

        @Override
        protected MockPathNode createNode( Segment name,
                                           Path parentPath,
                                           Iterable<Property> properties ) {
            return new MockPathNode(null, parentPath, name, properties, null);
        }

        public MockPathTransaction( Repository<MockPathNode, MockPathWorkspace> repository ) {
            super(repository, repository.getRootNodeUuid());
        }

        public boolean destroyWorkspace( MockPathWorkspace workspace ) throws InvalidWorkspaceException {
            return false;
        }

        public MockPathWorkspace getWorkspace( String name,
                                               MockPathWorkspace originalToClone ) throws InvalidWorkspaceException {
            MockPathWorkspace workspace = workspaces.get(name);

            if (workspace != null) {
                return workspace;
            }

            if (originalToClone != null) {
                workspace = new MockPathWorkspace(name, originalToClone);
            } else {
                workspace = new MockPathWorkspace(name, getRepository().getRootNodeUuid());
            }

            workspaces.put(name, workspace);
            return workspace;
        }

    }
}
