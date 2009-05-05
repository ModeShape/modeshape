package org.jboss.dna.jcr;

import javax.jcr.nodetype.NodeType;
import org.jboss.dna.graph.Graph;

/**
 * Interface for any potential provider of {@link JcrNodeType} definitions, the DNA implementation of {@link NodeType}. Possible
 * sources of node type definitions include CND files, repository metadata, and mock types for testing.
 * 
 * @see JcrWorkspace#getNodeTypeManager()
 */
public interface JcrNodeTypeSource {

    /**
     * Returns the node type information to be registered in graph form. The graph has a very specific required format.
     * <p>
     * The root node of the graph should have zero or more children. Each child of the root node represents a type to be
     * registered and the name of the node should be the name of the node type to be registered. Additionally, any facets of the
     * node type that are specified should be set in a manner consistent with the JCR specification for the {@code nt:nodeType}
     * built-in node type. The {@code jcr:primaryType} property does not need to be set on these nodes, but the nodes must be
     * semantically valid as if the {@code jcr:primaryType} property was set.
     * </p>
     * <p>
     * Each node type node may have zero or more children, each with the name {@code jcr:propertyDefinition} or {@code
     * jcr:childNodeDefinition}, as per the definition of the {@code nt:nodeType} built-in type. Each property definition and
     * child node definition must obey the semantics of {@code jcr:propertyDefinition} and {@code jcr:childNodeDefinition}
     * respectively However these nodes also do not need to have the {@code jcr:primaryType} property set.
     * </p>
     * <p>
     * For example, one valid graph is:
     * 
     * <pre>
     * &lt;root&gt;
     * +---- test:testMixinType
     *        +--- jcr:nodeTypeName               =  test:testMixinType  (PROPERTY)
     *        +--- jcr:isMixin                    =  true                (PROPERTY)    
     *        +--- jcr:childNodeDefinition                               (CHILD NODE)
     *        |     +--- jcr:name                 =  test:childNodeA     (PROPERTY)
     *        |     +--- jcr:mandatory            =  true                (PROPERTY)
     *        |     +--- jcr:autoCreated          =  true                (PROPERTY)
     *        |     +--- jcr:defaultPrimaryType   =  nt:base             (PROPERTY)
     *        |     +--- jcr:requiredPrimaryTypes =  nt:base             (PROPERTY)
     *        +--- jcr:propertyDefinition                                (CHILD NODE)
     *              +--- jcr:name                 =  test:propertyA      (PROPERTY)
     *              +--- jcr:multiple             =  true                (PROPERTY)
     *              +--- jcr:requiredType         =  String              (PROPERTY)
     * </pre>
     * 
     * This graph (when registered) would create a mixin node named "test:testMixinType" with a mandatory, autocreated child node
     * named "test:childNodeA" with a default and required primary type of "nt:base" and a multi-valued string property named
     * "test:propertyA". 
     * </p>
     * 
     * @return a graph with the semantics noted above
     */
    Graph getNodeTypes();
}
