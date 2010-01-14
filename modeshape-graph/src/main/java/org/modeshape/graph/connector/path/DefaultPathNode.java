package org.modeshape.graph.connector.path;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

/**
 * Default immutable implementation of {@link PathNode}
 */
@Immutable
public class DefaultPathNode implements PathNode {

    private final static TextEncoder TO_STRING_ENCODER = new NoOpEncoder();

    private final Path path;
    private final Map<Name, Property> properties;
    private final List<Segment> childSegments;
    private final UUID uuid;
    private Set<Name> uniqueChildNames;

    public DefaultPathNode( Path path,
                            UUID uuid,
                            Map<Name, Property> properties,
                            List<Segment> childSegments ) {
        super();
        this.path = path;
        this.uuid = uuid;
        this.properties = properties;
        this.childSegments = childSegments;
    }

    public DefaultPathNode( Path path,
                            UUID uuid,
                            Iterable<Property> properties,
                            List<Segment> childSegments ) {
        super();
        this.path = path;
        this.uuid = uuid;
        this.childSegments = childSegments;
        this.properties = new HashMap<Name, Property>();

        for (Property property : properties) {
            this.properties.put(property.getName(), property);
        }
    }

    public List<Segment> getChildSegments() {
        return this.childSegments;
    }

    public Path getPath() {
        return this.path;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public Map<Name, Property> getProperties() {
        return Collections.unmodifiableMap(this.properties);
    }

    public Property getProperty( ExecutionContext context,
                                 String name ) {
        NameFactory nameFactory = context.getValueFactories().getNameFactory();

        return getProperty(nameFactory.create(name));
    }

    public Property getProperty( Name name ) {
        return properties.get(name);
    }

    public Set<Name> getUniqueChildNames() {
        if (this.uniqueChildNames == null) {
            Set<Name> childNames = new HashSet<Name>(childSegments.size());

            for (Segment childSegment : childSegments) {
                childNames.add(childSegment.getName());
            }

            this.uniqueChildNames = childNames;
        }

        return null;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append(path.getString(TO_STRING_ENCODER)).append('(').append(properties.keySet()).append(')');
        return buff.toString();
    }

    
}
