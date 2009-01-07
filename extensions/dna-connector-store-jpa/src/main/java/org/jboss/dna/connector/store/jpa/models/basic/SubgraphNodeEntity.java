/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.store.jpa.models.basic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.hibernate.annotations.Index;

/**
 * Represents a single node that appears in a subgraph.
 * 
 * @author Randall Hauch
 * @see SubgraphQueryEntity
 */
@Entity
@Table( name = "DNA_SUBGRAPH_NODES" )
@org.hibernate.annotations.Table( appliesTo = "DNA_SUBGRAPH_NODES", indexes = @Index( name = "QUERYID_INX", columnNames = {
    "QUERY_ID", "UUID"} ) )
@NamedQueries( {
    @NamedQuery( name = "SubgraphNodeEntity.insertChildren", query = "insert into SubgraphNodeEntity(queryId,nodeUuid,depth,parentIndexInParent,indexInParent) select parentNode.queryId, child.id.childUuidString, parentNode.depth+1, parentNode.indexInParent, child.indexInParent from ChildEntity child, SubgraphNodeEntity parentNode where child.id.parentUuidString = parentNode.nodeUuid and parentNode.queryId = :queryId and parentNode.depth = :parentDepth" ),
    @NamedQuery( name = "SubgraphNodeEntity.getCount", query = "select count(*) from SubgraphNodeEntity where queryId = :queryId" ),
    @NamedQuery( name = "SubgraphNodeEntity.getPropertiesEntities", query = "select props from PropertiesEntity props, SubgraphNodeEntity node where props.id.uuidString = node.nodeUuid and node.queryId = :queryId and node.depth >= :depth and node.depth <= :maxDepth order by node.depth, node.parentIndexInParent, node.indexInParent" ),
    @NamedQuery( name = "SubgraphNodeEntity.getPropertiesEntitiesWithLargeValues", query = "select props from PropertiesEntity props, SubgraphNodeEntity node where props.id.uuidString = node.nodeUuid and node.queryId = :queryId and node.depth >= :depth and size(props.largeValues) > 0" ),
    @NamedQuery( name = "SubgraphNodeEntity.getChildEntities", query = "select child from ChildEntity child, SubgraphNodeEntity node where child.id.childUuidString = node.nodeUuid and node.queryId = :queryId and node.depth >= :depth and node.depth <= :maxDepth order by node.depth, node.parentIndexInParent, node.indexInParent" ),
    @NamedQuery( name = "SubgraphNodeEntity.getInternalReferences", query = "select ref from ReferenceEntity as ref where ref.id.toUuidString in ( select node.nodeUuid from SubgraphNodeEntity node where node.queryId = :queryId) and ref.id.fromUuidString in (select node.nodeUuid from SubgraphNodeEntity node where node.queryId = :queryId)" ),
    @NamedQuery( name = "SubgraphNodeEntity.getOutwardReferences", query = "select ref from ReferenceEntity as ref where ref.id.toUuidString not in ( select node.nodeUuid from SubgraphNodeEntity node where node.queryId = :queryId) and ref.id.fromUuidString in (select node.nodeUuid from SubgraphNodeEntity node where node.queryId = :queryId)" ),
    @NamedQuery( name = "SubgraphNodeEntity.getInwardReferences", query = "select ref from ReferenceEntity as ref where ref.id.toUuidString in ( select node.nodeUuid from SubgraphNodeEntity node where node.queryId = :queryId) and ref.id.fromUuidString not in (select node.nodeUuid from SubgraphNodeEntity node where node.queryId = :queryId)" ),
    @NamedQuery( name = "SubgraphNodeEntity.deletePropertiesEntities", query = "delete PropertiesEntity props where props.id.uuidString in ( select node.nodeUuid from SubgraphNodeEntity node where node.queryId = :queryId )" ),
    @NamedQuery( name = "SubgraphNodeEntity.deleteChildEntities", query = "delete ChildEntity child where child.id.childUuidString in ( select node.nodeUuid from SubgraphNodeEntity node where node.queryId = :queryId )" ),
    @NamedQuery( name = "SubgraphNodeEntity.deleteReferences", query = "delete ReferenceEntity as ref where ref.id.fromUuidString in ( select node.nodeUuid from SubgraphNodeEntity node where node.queryId = :queryId )" ),
    @NamedQuery( name = "SubgraphNodeEntity.deleteByQueryId", query = "delete SubgraphNodeEntity where queryId = :queryId" )} )
public class SubgraphNodeEntity {

    @Id
    @Column( name = "ID" )
    @GeneratedValue( strategy = GenerationType.AUTO )
    private Long id;

    @Column( name = "QUERY_ID", nullable = false, unique = false, updatable = false )
    private Long queryId;

    @Column( name = "UUID", updatable = false, nullable = false, length = 36 )
    private String nodeUuid;

    @Column( name = "DEPTH", updatable = false, nullable = false )
    private int depth;

    @Column( name = "PARENT_NUM", updatable = false, nullable = false )
    private int parentIndexInParent;

    @Column( name = "CHILD_NUM", updatable = false, nullable = false )
    private int indexInParent;

    public SubgraphNodeEntity() {
    }

    public SubgraphNodeEntity( Long queryId,
                               String nodeUuid,
                               int depth ) {
        this.queryId = queryId;
        this.nodeUuid = nodeUuid;
        this.depth = depth;
    }

    /**
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @return nodeUuid
     */
    public String getNodeUuid() {
        return nodeUuid;
    }

    /**
     * @return queryId
     */
    public Long getQueryId() {
        return queryId;
    }

    /**
     * @return indexInParent
     */
    public int getIndexInParent() {
        return indexInParent;
    }

    /**
     * @return parentIndexInParent
     */
    public int getParentIndexInParent() {
        return parentIndexInParent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id != null ? id.intValue() : 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SubgraphNodeEntity) {
            SubgraphNodeEntity that = (SubgraphNodeEntity)obj;
            if (this.id.equals(that.id)) return true;
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
        return "" + id + " - Query " + queryId + "; depth=" + depth + "; node=" + nodeUuid + " at index " + indexInParent;
    }

}
