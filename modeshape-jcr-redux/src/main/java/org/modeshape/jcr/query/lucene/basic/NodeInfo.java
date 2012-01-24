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

import java.util.Map;
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
 * A record object used to encapsulate the indexable information about a node.
 */
@Immutable
@Indexed( index = NodeInfoIndex.INDEX_NAME )
public class NodeInfo {

    /* The DocumentId fields are always stored */
    @DocumentId( name = NodeInfoIndex.FieldName.ID )
    private final String id;

    @Field( name = NodeInfoIndex.FieldName.WORKSPACE, analyze = Analyze.NO, store = Store.NO, index = Index.YES )
    private final String workspace;

    @Field( name = NodeInfoIndex.FieldName.PATH, analyze = Analyze.NO, store = Store.NO, index = Index.YES )
    private final String path;

    @Field( name = NodeInfoIndex.FieldName.NODE_NAME, analyze = Analyze.NO, store = Store.NO, index = Index.YES )
    private final String name;

    @Field( name = NodeInfoIndex.FieldName.LOCAL_NAME, analyze = Analyze.NO, store = Store.NO, index = Index.YES )
    private final String localName;

    @Field( name = NodeInfoIndex.FieldName.DEPTH, analyze = Analyze.NO, store = Store.NO, index = Index.YES )
    @NumericField( forField = NodeInfoIndex.FieldName.DEPTH )
    private final int depth;

    @Field( analyze = Analyze.NO, store = Store.NO )
    @FieldBridge( impl = NodeInfoBridge.class )
    private final Map<String, Object> properties;

    @Field( name = NodeInfoIndex.FieldName.FULL_TEXT, analyze = Analyze.YES, store = Store.NO, index = Index.YES )
    private final String fullText;

    public NodeInfo( String nodeKey,
                     String workspace,
                     String path,
                     String localName,
                     String name,
                     int depth,
                     Map<String, Object> properties,
                     String fullText ) {
        this.id = nodeKey;
        this.workspace = workspace;
        this.path = path;
        this.name = name;
        this.localName = localName;
        this.depth = depth;
        this.properties = properties;
        this.fullText = fullText;
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
     * Get the depth of the node.
     * 
     * @return the depth; always >= 0
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get the properties for the node. Note the type of each property value dictates how that property will be indexed.
     * 
     * @return the collection of property objects; never null
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * The name of the workspace.
     * 
     * @return the workspace name; never null
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Get the text terms that should be included in the {@link NodeInfoIndex.FieldName#FULL_TEXT full-text search field}. Note
     * that this should never include text from binary values.
     * 
     * @return the full-text search terms; may be null
     */
    public String getFullText() {
        return fullText;
    }

    @Override
    public String toString() {
        return id + " @ " + path + " in '" + workspace + "' with " + properties;
    }

}
