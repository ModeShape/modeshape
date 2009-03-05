package org.jboss.dna.jcr;

import java.util.Collection;
import javax.jcr.nodetype.NodeType;
import org.jboss.dna.graph.property.Name;

/**
 * Interface for any potential provider of {@link JcrNodeType} definitions, the DNA implementation of {@link NodeType}. Possible
 * sources of node type definitions include CND files, repository metadata, and mock types for testing.
 * 
 * @see JcrWorkspace#getNodeTypeManager()
 */
public interface JcrNodeTypeSource {

    /**
     * Returns the list of primary node types provided by this source
     * 
     * @return the list of primary node types provided by this source
     */
    public Collection<JcrNodeType> getPrimaryNodeTypes();

    /**
     * Returns the list of mixin node types provided by this source
     * 
     * @return the list of mixin node types provided by this source
     */
    public Collection<JcrNodeType> getMixinNodeTypes();

    /**
     * Finds the type with the given name and returns its definition.
     * @param typeName the name of the type to return
     * @return the type named <code>typeName</code> if it exists, otherwise <code>null</code>.
     */
    public JcrNodeType findType(Name typeName);
}
