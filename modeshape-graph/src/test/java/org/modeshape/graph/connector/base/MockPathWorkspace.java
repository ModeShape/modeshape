package org.modeshape.graph.connector.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

public class MockPathWorkspace extends PathWorkspace<MockPathNode> {

    protected ExecutionContext context = new ExecutionContext();
    protected InternalNode rootNode;

    public MockPathWorkspace( String name,
                              UUID rootNodeUuid ) {
        super(name, rootNodeUuid);
    }

    public MockPathWorkspace( String name,
                              MockPathWorkspace originalToClone ) {
        super(name, originalToClone);
    }

    protected PathFactory pathFactory() {
        return context.getValueFactories().getPathFactory();
    }

    private InternalNode nodeAt( Path path ) {
        InternalNode node = rootNode();

        for (Segment segment : path) {
            node = node.getChild(segment);
            if (node == null) return null;
        }
        return node;
    }

    @Override
    public MockPathNode getNode( Path path ) {

        if (nodeAt(path) == null) {
            assert nodeAt(path) != null;
        }

        return pathNodeFor(nodeAt(path));
    }

    private InternalNode rootNode() {
        if (rootNode == null) {
            rootNode = new InternalNode(getRootNodeUuid());
        }

        return rootNode;
    }

    @Override
    public MockPathNode getRootNode() {
        return pathNodeFor(rootNode());
    }

    private MockPathNode pathNodeFor(InternalNode node) {
        if (node.getParent() == null) {
            new MockPathNode(node.getUuid(), null, node.getName(), node.getProperties(), node.getChildren());
        }
        return new MockPathNode(node.getUuid(), node.getPath().getParent(), node.getName(), node.getProperties(),
                                node.getChildren());
    }

    @Override
    public MockPathNode putNode( MockPathNode node ) {
        InternalNode target;
        if (node.getParent() == null) {
            target = rootNode;
        }
        else {
            InternalNode parent = nodeAt(node.getParent());
            target = parent.getChild(node.getName());
            
            if (target == null) {
                target = new InternalNode(parent, node.getName(), node.getProperties(), null);
                parent.addChild(target);
                return pathNodeFor(target);
            }
        }
        
        target.setProperties(node.getProperties());
        
        return pathNodeFor(target);
    }

    @Override
    public MockPathNode moveNode( MockPathNode node,
                                  MockPathNode newNode ) {
        InternalNode parent = nodeAt(newNode.getParent());

        InternalNode child = nodeAt(pathFactory().create(node.getParent(), node.getName()));

        parent.addChild(child);

        return pathNodeFor(child);
    }

    @Override
    public void removeAll() {
        rootNode = null;
    }

    @Override
    public MockPathNode removeNode( Path path ) {
        if (path.isRoot()) {
            InternalNode oldRoot = rootNode;
            removeAll();
            return pathNodeFor(oldRoot);
        }
        
        InternalNode target = nodeAt(path);
        InternalNode parent = target.getParent();
        
        parent.removeChild(target.getName());

        return pathNodeFor(target);
    }

    private class InternalNode {
        private UUID uuid;
        private Segment name;
        private InternalNode parent;
        private Map<Name, Property> properties;
        private List<InternalNode> children;

        protected InternalNode( UUID uuid ) {
            this(uuid, null, null, null, null);
        }

        protected InternalNode( InternalNode parent,
                              Segment name,
                              Map<Name, Property> properties,
                              List<InternalNode> children ) {
            this(null, parent, name, properties, children);
        }

        protected InternalNode( UUID uuid,
                              InternalNode parent,
                              Segment name,
                              Map<Name, Property> properties,
                              List<InternalNode> children ) {
            this.uuid = uuid;
            this.parent = parent;
            this.name = name;
            this.properties = properties == null ? new HashMap<Name, Property>() : new HashMap<Name, Property>(properties);
            this.children = children == null ? new ArrayList<InternalNode>() : children;
        }

        protected InternalNode getChild( Segment segment ) {
            for (InternalNode node : children) {
                if (node.getName().equals(segment)) return node;
            }
            return null;
        }

        protected void removeChild( Segment segment ) {
            children.remove(segment);
        }

        protected void addChild( InternalNode child ) {
            children.add(child);
            child.parent = this;
        }

        protected List<Segment> getChildren() {
            List<Segment> childSegments = new ArrayList<Segment>(children.size());

            for (InternalNode child : children) {
                childSegments.add(child.getName());
            }

            return childSegments;
        }

        protected Map<Name, Property> getProperties() {
            return new HashMap<Name, Property>(properties);
        }

        protected void setProperties( Map<Name, Property> properties ) {
            this.properties = new HashMap<Name, Property>(properties);
        }

        protected InternalNode getParent() {
            return parent;
        }

        protected Segment getName() {
            return name;
        }

        protected UUID getUuid() {
            return uuid;
        }

        protected Path getPath() {
            if (parent == null) return pathFactory().createRootPath();

            return pathFactory().create(parent.getPath(), name);
        }

        @Override
        public String toString() {
            return getPath().getString(context.getNamespaceRegistry());
        }
    }
}
