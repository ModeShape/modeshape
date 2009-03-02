package org.jboss.dna.jcr;

import java.util.Collection;
import javax.jcr.nodetype.NodeType;

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

}
