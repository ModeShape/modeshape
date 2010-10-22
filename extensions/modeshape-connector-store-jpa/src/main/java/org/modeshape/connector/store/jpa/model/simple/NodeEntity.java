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
package org.modeshape.connector.store.jpa.model.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Query;
import javax.persistence.Table;
import org.hibernate.annotations.Index;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.HashCode;
import org.modeshape.connector.store.jpa.model.common.NamespaceEntity;
import org.modeshape.connector.store.jpa.util.Serializer;

/**
 * An entity a node and its properties. In addition to the references to the parent and child nodes, this entity also maintains
 * the indexInParent of the indexInParent within the parent node's list of all children, the child's name (
 * {@link #getChildName() local part} and {@link #getChildNamespace() namespace}), and the same-name-sibling indexInParent (if
 * there is one).
 */
@Entity
@org.hibernate.annotations.Table( appliesTo = "MODE_SIMPLE_NODE", indexes = {
    @Index( name = "NODEUUID_INX", columnNames = {"WORKSPACE_ID", "NODE_UUID"} ),
    @Index( name = "CHILDINDEX_INX", columnNames = {"WORKSPACE_ID", "PARENT_ID", "CHILD_INDEX"} ),
    @Index( name = "CHILDNAME_INX", columnNames = {"WORKSPACE_ID", "PARENT_ID", "CHILD_NAME_NS_ID", "CHILD_NAME_LOCAL",
        "SNS_INDEX"} )} )
@Table( name = "MODE_SIMPLE_NODE" )
@NamedQueries( {
    @NamedQuery( name = "NodeEntity.findByNodeUuid", query = "from NodeEntity as node where node.workspaceId = :workspaceId and node.nodeUuidString = :nodeUuidString" ),
    @NamedQuery( name = "NodeEntity.findInWorkspace", query = "from NodeEntity as node where node.workspaceId = :workspaceId" ),
    @NamedQuery( name = "NodeEntity.deleteAllInWorkspace", query = "delete from NodeEntity where workspaceId = :workspaceId" ),
    @NamedQuery( name = "NodeEntity.withLargeValues", query = "from NodeEntity as node where node.workspaceId = :workspaceId and size(node.largeValues) > 0" )} )
public class NodeEntity {

    @Id
    @GeneratedValue( strategy = GenerationType.AUTO )
    @Column( name = "ID" )
    private long id;

    @Column( name = "WORKSPACE_ID", nullable = false )
    private long workspaceId;

    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "PARENT_ID", referencedColumnName = "id", nullable = true )
    private NodeEntity parent;

    @Column( name = "NODE_UUID", nullable = false, length = 36 )
    private String nodeUuidString;

    /** The zero-based index */
    @Column( name = "CHILD_INDEX", nullable = false, unique = false )
    private int indexInParent = 0;

    @ManyToOne( fetch = FetchType.LAZY )
    @JoinColumn( name = "CHILD_NAME_NS_ID", nullable = true )
    private NamespaceEntity childNamespace;

    @Column( name = "CHILD_NAME_LOCAL", nullable = true, unique = false, length = 512 )
    private String childName;

    @Column( name = "SNS_INDEX", nullable = false, unique = false )
    private int sameNameSiblingIndex = 1;

    @OneToMany( fetch = FetchType.LAZY, mappedBy = "parent" )
    @OrderBy( "indexInParent" )
    private final List<NodeEntity> children = new ArrayList<NodeEntity>();
    /**
     * Tracks whether this node allows or disallows its children to have the same names (to be same-name-siblings). The model uses
     * this to know whether it can optimization the database operations when creating, inserting, or removing children.
     */
    @Column( name = "ALLOWS_SNS", nullable = false, unique = false )
    private boolean allowsSameNameChildren;

    @Lob
    @Column( name = "DATA", nullable = true, unique = false )
    private byte[] data;

    @Column( name = "NUM_PROPS", nullable = false )
    private int propertyCount;

    /**
     * Flag specifying whether the binary data is stored in a compressed format.
     */
    @Column( name = "COMPRESSED", nullable = true )
    private Boolean compressed;

    /**
     * Flag specifying whether this node should be included in referential integrity enforcement.
     */
    @Column( name = "ENFORCEREFINTEG", nullable = false )
    private boolean referentialIntegrityEnforced = true;

    // @org.hibernate.annotations.CollectionOfElements( fetch = FetchType.LAZY )
    // @JoinTable( name = "ModeShape_LARGEVALUE_USAGES", joinColumns = {@JoinColumn( name = "WORKSPACE_ID" ),
    // @JoinColumn( name = "NODE_UUID" )} )
    @ManyToMany
    @JoinTable( name = "ModeShape_LARGEVALUE_USAGES", joinColumns = {@JoinColumn( name = "ID" )} )
    private final Set<LargeValueEntity> largeValues = new HashSet<LargeValueEntity>();

    public NodeEntity() {
    }

    public NodeEntity( long id,
                       NodeEntity parent,
                       String nodeUuidString,
                       long workspaceId,
                       int indexInParent,
                       NamespaceEntity ns,
                       String name ) {
        this.id = id;
        this.parent = parent;
        this.nodeUuidString = nodeUuidString;
        this.workspaceId = workspaceId;
        this.indexInParent = indexInParent;
        this.childNamespace = ns;
        this.childName = name;
        this.sameNameSiblingIndex = 1;
    }

    public NodeEntity( long id,
                       NodeEntity parent,
                       String nodeUuidString,
                       long workspaceId,
                       int indexInParent,
                       NamespaceEntity ns,
                       String name,
                       int sameNameSiblingIndex ) {
        this.id = id;
        this.parent = parent;
        this.nodeUuidString = nodeUuidString;
        this.workspaceId = workspaceId;
        this.indexInParent = indexInParent;
        this.childNamespace = ns;
        this.childName = name;
        this.sameNameSiblingIndex = sameNameSiblingIndex;
    }

    /**
     * Returns this node's unique identifier
     * 
     * @return this node's unique identifier
     */
    public long getNodeId() {
        return id;
    }

    /**
     * @param id Sets this node's unique identifier
     */
    public void setNodeId( long id ) {
        this.id = id;
    }

    /**
     * Returns the parent identifier
     * 
     * @return the parent identifier
     */
    public NodeEntity getParent() {
        return parent;
    }

    /**
     * Sets the parent identifier
     * 
     * @param parent the parent identifier
     */
    public void setParent( NodeEntity parent ) {
        this.parent = parent;
    }

    /**
     * Returns the node UUID string
     * 
     * @return the node UUID string
     */
    public String getNodeUuidString() {
        return nodeUuidString;
    }

    /**
     * Sets the node UUID string
     * 
     * @param nodeUuidString the node UUID string
     */
    public void setNodeUuidString( String nodeUuidString ) {
        this.nodeUuidString = nodeUuidString;
    }

    /**
     * Returns the identifier of the workspace containing this node
     * 
     * @return the identifier of the workspace containing this node
     */
    public long getWorkspaceId() {
        return workspaceId;
    }

    /**
     * Sets the identifier of the workspace containing this node
     * 
     * @param workspaceId the identifier of the workspace containing this node
     */
    public void setWorkspaceId( long workspaceId ) {
        this.workspaceId = workspaceId;
    }

    /**
     * Get the zero-based index of this child within the parent's list of children
     * 
     * @return the zero-based index of this child
     */
    public int getIndexInParent() {
        return indexInParent;
    }

    /**
     * @param index Sets indexInParent to the specified value.
     */
    public void setIndexInParent( int index ) {
        this.indexInParent = index;
    }

    /**
     * @return childName
     */
    public String getChildName() {
        return childName;
    }

    /**
     * @param childName Sets childName to the specified value.
     */
    public void setChildName( String childName ) {
        this.childName = childName;
    }

    /**
     * @return childNamespace
     */
    public NamespaceEntity getChildNamespace() {
        return childNamespace;
    }

    /**
     * @param childNamespace Sets childNamespace to the specified value.
     */
    public void setChildNamespace( NamespaceEntity childNamespace ) {
        this.childNamespace = childNamespace;
    }

    /**
     * @return sameNameSiblingIndex
     */
    public int getSameNameSiblingIndex() {
        return sameNameSiblingIndex;
    }

    /**
     * @param sameNameSiblingIndex Sets sameNameSiblingIndex to the specified value.
     */
    public void setSameNameSiblingIndex( int sameNameSiblingIndex ) {
        this.sameNameSiblingIndex = sameNameSiblingIndex;
    }

    /**
     * @return allowsSameNameChildren
     */
    public boolean getAllowsSameNameChildren() {
        return allowsSameNameChildren;
    }

    /**
     * @param allowsSameNameChildren Sets allowsSameNameChildren to the specified value.
     */
    public void setAllowsSameNameChildren( boolean allowsSameNameChildren ) {
        this.allowsSameNameChildren = allowsSameNameChildren;
    }

    /**
     * Get the data that represents the {@link Serializer packed} properties.
     * 
     * @return the raw data representing the properties
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Set the data that represents the {@link Serializer packed} properties.
     * 
     * @param data the raw data representing the properties
     */
    public void setData( byte[] data ) {
        this.data = data;
    }

    /**
     * @return propertyCount
     */
    public int getPropertyCount() {
        return propertyCount;
    }

    /**
     * @param propertyCount Sets propertyCount to the specified value.
     */
    public void setPropertyCount( int propertyCount ) {
        this.propertyCount = propertyCount;
    }

    /**
     * @return compressed
     */
    public boolean isCompressed() {
        return compressed != null && compressed.booleanValue();
    }

    /**
     * @param compressed Sets compressed to the specified value.
     */
    public void setCompressed( boolean compressed ) {
        this.compressed = Boolean.valueOf(compressed);
    }

    public List<NodeEntity> getChildren() {
        return children;
    }

    public void addChild( NodeEntity child ) {
        children.add(child);
        child.setIndexInParent(children.size() - 1);
    }

    public void addChild( int index,
                          NodeEntity child ) {
        for (NodeEntity existing : children.subList(index, children.size() - 1)) {
            existing.setIndexInParent(existing.getIndexInParent() + 1);
        }

        children.add(index, child);
        child.setIndexInParent(index);
    }

    public boolean removeChild( int index ) {
        NodeEntity removedNode = children.remove(index);
        if (removedNode == null) return false;
        removedNode.setParent(null);

        if (index < children.size()) {
            for (NodeEntity child : children.subList(index, children.size() - 1)) {
                child.setIndexInParent(child.getIndexInParent() - 1);
            }
        }
        return true;
    }

    /**
     * @return largeValues
     */
    public Collection<LargeValueEntity> getLargeValues() {
        return largeValues;
    }

    /**
     * @return referentialIntegrityEnforced
     */
    public boolean isReferentialIntegrityEnforced() {
        return referentialIntegrityEnforced;
    }

    /**
     * @param referentialIntegrityEnforced Sets referentialIntegrityEnforced to the specified value.
     */
    public void setReferentialIntegrityEnforced( boolean referentialIntegrityEnforced ) {
        this.referentialIntegrityEnforced = referentialIntegrityEnforced;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(id);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof NodeEntity) {
            NodeEntity that = (NodeEntity)obj;
            if (this.id != that.id) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (childNamespace != null) {
            sb.append('{').append(childNamespace).append("}:");
        }
        sb.append(childName);
        if (sameNameSiblingIndex > 1) {
            sb.append('[').append(sameNameSiblingIndex).append(']');
        }
        sb.append(" (id=").append(getNodeUuidString()).append(")");
        if (parent != null) {
            sb.append(" is ");
            sb.append(Inflector.getInstance().ordinalize(indexInParent));
            sb.append(" child of ");
            sb.append(parent.getNodeId());
            sb.append(" in workspace ");
            sb.append(getWorkspaceId());
        } else {
            sb.append(" is root in workspace ");
            sb.append(getWorkspaceId());
        }
        return sb.toString();
    }

    @SuppressWarnings( "unchecked" )
    public static void adjustSnsIndexesAndIndexesAfterRemoving( EntityManager entities,
                                                                Long workspaceId,
                                                                String uuidParent,
                                                                String childName,
                                                                long childNamespaceIndex,
                                                                int childIndex ) {
        // Decrement the 'indexInParent' index values for all nodes above the previously removed sibling ...
        Query query = entities.createNamedQuery("NodeEntity.findChildrenAfterIndexUnderParent");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("parentUuidString", uuidParent);
        query.setParameter("afterIndex", childIndex);
        for (NodeEntity entity : (List<NodeEntity>)query.getResultList()) {
            // Decrement the index in parent ...
            entity.setIndexInParent(entity.getIndexInParent() - 1);
            if (entity.getChildName().equals(childName) && entity.getChildNamespace().getId() == childNamespaceIndex) {
                // The name matches, so decrement the SNS index ...
                entity.setSameNameSiblingIndex(entity.getSameNameSiblingIndex() - 1);
            }
        }
    }

    @SuppressWarnings( "unchecked" )
    public static int adjustSnsIndexesAndIndexes( EntityManager entities,
                                                  Long workspaceId,
                                                  String uuidParent,
                                                  int afterIndex,
                                                  int untilIndex,
                                                  long childNamespaceIndex,
                                                  String childName,
                                                  int modifier ) {
        int snsCount = 0;

        // Decrement the 'indexInParent' index values for all nodes above the previously removed sibling ...
        Query query = entities.createNamedQuery("NodeEntity.findChildrenAfterIndexUnderParent");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("parentUuidString", uuidParent);
        query.setParameter("afterIndex", afterIndex);

        int index = afterIndex;
        for (NodeEntity entity : (List<NodeEntity>)query.getResultList()) {
            if (++index > untilIndex) {
                break;
            }

            // Decrement the index in parent ...
            entity.setIndexInParent(entity.getIndexInParent() + modifier);
            if (entity.getChildName().equals(childName) && entity.getChildNamespace().getId() == childNamespaceIndex) {
                // The name matches, so decrement the SNS index ...
                entity.setSameNameSiblingIndex(entity.getSameNameSiblingIndex() + modifier);
                snsCount++;
            }
        }

        return snsCount;
    }
}
