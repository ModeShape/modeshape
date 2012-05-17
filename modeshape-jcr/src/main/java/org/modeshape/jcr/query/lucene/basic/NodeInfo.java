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
package org.modeshape.jcr.query.lucene.basic;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;
import org.modeshape.common.annotation.Immutable;

/**
 * A record object used to encapsulate the indexable information about a node. Some fields are known ahead of time, such as the
 * {@link #id}, {@link #name}, {@link #localName}, {@link #depth}, etc., but other fields are used to store the properties that
 * cannot be known at compile time. These fields are represented on this object using {@link DynamicField} instances, which can be
 * chained together.
 */
@Immutable
@Indexed( index = NodeInfoIndex.INDEX_NAME )
public class NodeInfo {

    /* The DocumentId fields are always stored */
    @DocumentId( name = NodeInfoIndex.FieldName.ID )
    private final String id;

    @Field( name = NodeInfoIndex.FieldName.WORKSPACE, analyze = Analyze.NO, store = Store.YES, index = Index.YES )
    private final String workspace;

    @Field( name = NodeInfoIndex.FieldName.PATH, analyze = Analyze.NO, store = Store.YES, index = Index.YES )
    private final String path;

    @Field( name = NodeInfoIndex.FieldName.NODE_NAME, analyze = Analyze.NO, store = Store.YES, index = Index.YES )
    private final String name;

    @Field( name = NodeInfoIndex.FieldName.LOCAL_NAME, analyze = Analyze.NO, store = Store.YES, index = Index.YES )
    private final String localName;

    @Field( name = NodeInfoIndex.FieldName.SNS_INDEX, analyze = Analyze.NO, store = Store.YES, index = Index.YES )
    @NumericField( forField = NodeInfoIndex.FieldName.SNS_INDEX )
    private final int snsIndex;

    @Field( name = NodeInfoIndex.FieldName.DEPTH, analyze = Analyze.NO, store = Store.NO, index = Index.YES )
    @NumericField( forField = NodeInfoIndex.FieldName.DEPTH )
    private final int depth;

    @Field( analyze = Analyze.YES, store = Store.NO )
    @FieldBridge( impl = DynamicFieldBridge.class )
    private final DynamicField firstDynamicField;

    public NodeInfo( String nodeKey,
                     String workspace,
                     String path,
                     String localName,
                     String name,
                     int snsIndex,
                     int depth,
                     DynamicField firstDynamicField ) {
        this.id = nodeKey;
        this.workspace = workspace;
        this.path = path;
        this.name = name;
        this.localName = localName;
        this.snsIndex = snsIndex;
        this.depth = depth;
        this.firstDynamicField = firstDynamicField;
    }

    /**
     * Get the unique ID for the node.
     * 
     * @return the id; never null
     */
    public String getId() {
        return id;
    }

    /**
     * Get the string path.
     * 
     * @return the path; never null
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the name of the node.
     * 
     * @return the name; never null
     */
    public String getName() {
        return name;
    }

    /**
     * Get the local name of the node.
     * 
     * @return the local name; never null
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Get the same-name-sibling index of the node.
     * 
     * @return the SNS index
     */
    public int getSnsIndex() {
        return snsIndex;
    }

    /**
     * Get the depth of the node.
     * 
     * @return the depth; always >= 0
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get the information about the first dynamic field for this node. Additional dynamic fields are changed and can be access
     * via the {@link DynamicField#getNext()} method.
     * 
     * @return the first dynamic field; may be null
     */
    public DynamicField getFirstDynamicField() {
        return firstDynamicField;
    }

    /**
     * The name of the workspace.
     * 
     * @return the workspace name; never null
     */
    public String getWorkspace() {
        return workspace;
    }

    @Override
    public String toString() {
        return id + " @ " + path + " in '" + workspace + "' with " + firstDynamicField;
    }

}
