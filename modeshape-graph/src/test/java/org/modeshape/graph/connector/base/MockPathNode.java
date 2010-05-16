package org.modeshape.graph.connector.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

@SuppressWarnings( "serial" )
public class MockPathNode extends PathNode {

    public MockPathNode( UUID uuid,
                         Path parent,
                         Segment name,
                         Iterable<Property> properties,
                         List<Segment> children ) {
        super(uuid, parent, name, properties, children);
    }

    public MockPathNode( UUID uuid,
                         Path parent,
                         Segment name,
                         Map<Name, Property> properties,
                         List<Segment> children,
                         int version ) {
        super(uuid, parent, name, properties, children, version);
    }

    public MockPathNode( UUID uuid,
                         Path parent,
                         Segment name,
                         Map<Name, Property> properties,
                         List<Segment> children ) {
        super(uuid, parent, name, properties, children);
    }

    public MockPathNode( UUID uuid ) {
        super(uuid);
    }

    @Override
    public PathNode clone() {
        return new MockPathNode(getUuid(), getParent(), getName(), new HashMap<Name, Property>(getProperties()),
                                new ArrayList<Segment>(getChildren()));
    }

    @Override
    public PathNode freeze() {
        if (!hasChanges()) return this;
        return new MockPathNode(getUuid(), changes.getParent(), changes.getName(), changes.getUnmodifiableProperties(),
                                changes.getUnmodifiableChildren(), getVersion() + 1);
    }

}
