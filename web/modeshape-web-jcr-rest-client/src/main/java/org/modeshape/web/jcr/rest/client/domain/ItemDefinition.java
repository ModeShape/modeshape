/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.web.jcr.rest.client.domain;

import java.util.Collection;
import java.util.Map;
import org.modeshape.common.annotation.Immutable;

/**
 * An immutable representation of an abstract JCR ItemDefinition.
 */
@Immutable
public abstract class ItemDefinition implements javax.jcr.nodetype.ItemDefinition {

    protected static String[] toArray( Collection<String> values ) {
        if (values == null) return null;
        if (values.isEmpty()) return new String[0];
        return values.toArray(new String[values.size()]);
    }

    protected static javax.jcr.nodetype.NodeType[] nodeTypes( Collection<String> nodeTypeNames,
                                                              Map<String, NodeType> nodeTypes ) {
        if (nodeTypes == null || nodeTypeNames == null || nodeTypeNames.isEmpty()) return new javax.jcr.nodetype.NodeType[0];
        int numValues = nodeTypeNames.size();
        int i = 0;
        NodeType[] result = new NodeType[numValues];
        for (String requiredTypeName : nodeTypeNames) {
            result[i++] = nodeTypes.get(requiredTypeName);
        }
        return result;
    }

    private final String declaringNodeTypeName;
    private final boolean isAutoCreated;
    private final boolean isMandatory;
    private final boolean isProtected;
    private final int onParentVersion;
    private final Map<String, NodeType> nodeTypes;

    protected ItemDefinition( String declaringNodeTypeName,
                              boolean isAutoCreated,
                              boolean isMandatory,
                              boolean isProtected,
                              int onParentVersion,
                              Map<String, NodeType> nodeTypes ) {
        assert declaringNodeTypeName != null;
        this.declaringNodeTypeName = declaringNodeTypeName;
        this.isAutoCreated = isAutoCreated;
        this.isMandatory = isMandatory;
        this.isProtected = isProtected;
        this.onParentVersion = onParentVersion;
        this.nodeTypes = nodeTypes;
    }

    /**
     * Find the node type with the supplied name.
     * 
     * @param name the name of the node type to find.
     * @return the named node type, or null if the node type is not known (or there are no node types known)
     */
    protected NodeType nodeType( String name ) {
        return nodeTypes != null ? nodeTypes.get(name) : null;
    }

    /**
     * @return nodeTypes
     */
    protected Map<String, NodeType> nodeTypes() {
        return nodeTypes;
    }

    /**
     * Get the name of the node type that declares this definition.
     * 
     * @return the node type name; never null
     */
    public String getDeclaringNodeTypeName() {
        return declaringNodeTypeName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
     */
    @Override
    public NodeType getDeclaringNodeType() {
        return nodeType(declaringNodeTypeName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#getOnParentVersion()
     */
    @Override
    public int getOnParentVersion() {
        return onParentVersion;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isAutoCreated()
     */
    @Override
    public boolean isAutoCreated() {
        return isAutoCreated;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isMandatory()
     */
    @Override
    public boolean isMandatory() {
        return isMandatory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.ItemDefinition#isProtected()
     */
    @Override
    public boolean isProtected() {
        return isProtected;
    }

}
