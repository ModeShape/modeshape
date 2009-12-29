package org.jboss.dna.graph.connector.path;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.Path.Segment;

/**
 * Default immutable implementation of {@link PathNode}
 */
@Immutable
public class DefaultPathNode implements PathNode {

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


}
