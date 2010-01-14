package org.modeshape.graph.connector.path;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * Basic interface for a read-only node in a {@link PathRepository path repository}.
 */
public interface PathNode {

    /**
     * Returns the full path to this node
     * 
     * @return the full path to this node
     */
    public Path getPath();

    /**
     * Returns the UUID for this node. Only the root node in a {@link PathWorkspace} should have a UUID. All other nodes should
     * return null from this method.
     * 
     * @return the UUID for this node; may be null
     */
    public UUID getUuid();

    /**
     * Returns the set of child names for this node
     * 
     * @return the set of child names for this node
     */
    public Set<Name> getUniqueChildNames();

    /**
     * @return children
     */
    public List<Path.Segment> getChildSegments();

    /**
     * Returns the named property
     * 
     * @param context the current execution context, used to get a {@link NameFactory name factory}
     * @param name the name of the property to return
     * @return the property for the given name
     */
    public Property getProperty( ExecutionContext context,
                                 String name );

    /**
     * Returns the named property
     * 
     * @param name the name of the property to return
     * @return the property for the given name
     */
    public Property getProperty( Name name );

    /**
     * Returns a map of property names to the property for the given name
     * 
     * @return a map of property names to the property for the given name
     */
    public Map<Name, Property> getProperties();
}
