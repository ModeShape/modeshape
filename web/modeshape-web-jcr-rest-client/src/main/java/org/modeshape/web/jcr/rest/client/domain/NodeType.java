/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.web.jcr.rest.client.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import net.jcip.annotations.Immutable;

import org.modeshape.common.util.HashCode;
import org.modeshape.web.jcr.rest.client.RestClientI18n;

/**
 * The NodeType class is the business object for a ModeShape supported node type.
 */
@Immutable
public class NodeType implements IModeShapeObject {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The node type name.
     */
    private final String name;

    /**
     * The workspace where this node type resides.
     */
    private final Workspace workspace;
    
    private Properties properties = null;
    
    private NodeType parentNodeType = null;
    
    private List<NodeType> childrenNodeType = null;
    
    private List<NodeType> propertyDefinitons = null;
    
    private List<NodeType> childNodeDefinitons = null;
    
    private List<NodeType> superTypes = null;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Constructs a new <code>NodeType</code>.
     * 
     * @param name the node type name (never <code>null</code>)
     * @param workspace the workspace where this node type resides (never <code>null</code>)
     * @param properties which are the attributes defined for this node type (<code>nullable</code>)
     * @throws IllegalArgumentException if the name or workspace argument is <code>null</code>
     */
    public NodeType( String name,
    		Workspace workspace,
    		Properties properties) {
    	assert name != null;
    	assert workspace != null;
     	this.name = name;
        this.workspace = workspace;
        this.properties = properties;  

     }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if ((obj == null) || (getClass() != obj.getClass())) return false;

        NodeType otherNodeType = (NodeType)obj;
        return (this.name.equals(otherNodeType.name) && this.workspace.equals(otherNodeType.workspace));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getName()
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the server where this workspace is located (never <code>null</code>)
     */
    public Workspace getWorkspace() {
        return this.workspace;
    }
    
    /**
     * @return the node type attributes as a property set.
     */
    public Properties getProperties() {
    	return (this.properties != null ? properties : new Properties());
    }
    
    public void setProperties(Properties properties ){
    	this.properties = properties;
    }
    
    public String getProperty(String key) {
    	return (this.properties != null ? this.properties.getProperty(key) : null);
    }
    
    @SuppressWarnings("unchecked")
	public List<NodeType> getChildren() {
    	return (List<NodeType>) (this.childrenNodeType != null ? this.childrenNodeType : Collections.emptyList());
    }
    
    public void addChildNodeType(NodeType childNodeType) {
		if (this.childrenNodeType == null) this.childrenNodeType = new ArrayList<NodeType>();
		this.childrenNodeType.add(childNodeType);
    	
		childNodeType.setParentNodeType(this);
    }
    
    public void addPropertyDefinitionNodeType(NodeType propertyDefinitionNodeType) {
		if (this.propertyDefinitons == null) this.propertyDefinitons = new ArrayList<NodeType>();
		propertyDefinitons.add(propertyDefinitionNodeType);

		propertyDefinitionNodeType.setParentNodeType(this);
    }
    
    public void addChildNodeDefinitionNodeType(NodeType childNodeDefinitionNodeType) {
		if (this.childNodeDefinitons == null) this.childNodeDefinitons = new ArrayList<NodeType>();
		childNodeDefinitons.add(childNodeDefinitionNodeType);

		childNodeDefinitionNodeType.setParentNodeType(this);
    }
    
    public void addSuperNodeType(NodeType superNodeType) {
		if (this.superTypes == null) this.superTypes = new ArrayList<NodeType>();
		superTypes.add(superNodeType);
   }
    
    @SuppressWarnings("unchecked")
	public List<NodeType> getPropertyDefinitions() {
    	return (List<NodeType>) (this.propertyDefinitons != null ? this.propertyDefinitons : Collections.emptyList());
    }
    
    @SuppressWarnings("unchecked")
	public List<NodeType> getChildNodeDefinitions() {
    	return (List<NodeType>) (this.childNodeDefinitons != null ? this.childNodeDefinitons : Collections.emptyList());
    }
    
    @SuppressWarnings("unchecked")
	public List<NodeType> getSuperNodeTypes() {
    	return (List<NodeType>) (this.superTypes != null ? this.superTypes : Collections.emptyList());
    }
    
    public NodeType getParentNodeType() {
    	return this.parentNodeType;
    }
    
    public void setParentNodeType(NodeType parent) {
    	this.parentNodeType = parent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.web.jcr.rest.client.domain.IModeShapeObject#getShortDescription()
     */
    public String getShortDescription() {
        return RestClientI18n.nodeTypeShortDescription.text(this.name, this.workspace.getServer());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(this.name, this.workspace);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getShortDescription();
    }
    
}
