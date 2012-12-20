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

package org.modeshape.jcr.federation.spi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * A writer which can create and manipulate the structure of {@link EditableDocument} instances, that should be used by connectors
 * when they want to return the information regarding external nodes.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface DocumentWriter extends PageWriter {

    /**
     * Sets an identifier string on the underlying document.
     * 
     * @param id the identifier of the document; may not be null
     * @return this writer; never null
     */
    DocumentWriter setId( String id );

    /**
     * Sets the primary type of the underlying document.
     * 
     * @param name the name of the primary type for the node; may not be null
     * @return this writer; never null
     */
    DocumentWriter setPrimaryType( String name );

    /**
     * Sets the primary type of the underlying document.
     * 
     * @param name the name of the primary type for the node; may not be null
     * @return this writer; never null
     */
    DocumentWriter setPrimaryType( Name name );

    /**
     * Adds a mixin type to the underlying document.
     * 
     * @param name the name of a node type that should be added as a mixin for the node; may not be null
     * @return this writer; never null
     */
    DocumentWriter addMixinType( String name );

    /**
     * Adds a mixin type to the underlying document.
     * 
     * @param name the name of a node type that should be added as a mixin for the node; may not be null
     * @return this writer; never null
     */
    DocumentWriter addMixinType( Name name );

    /**
     * Adds a property with the given name and value to the underlying document's properties.
     * 
     * @param name the name of the property; may not be null
     * @param value the value for the property; may not be null
     * @return this writer; never null
     */
    DocumentWriter addProperty( String name,
                                Object value );

    /**
     * Adds a property with the given name and value to the underlying document's properties.
     * 
     * @param name the name of the property; may not be null
     * @param value a value for the property; may not be null
     * @return this writer; never null
     */
    DocumentWriter addProperty( Name name,
                                Object value );

    /**
     * Adds a multi-value property with the given name to the underlying document's properties.
     * 
     * @param name a name of the property; may not be null
     * @param values the values for the property; may not be null but may be empty
     * @return this writer; never null
     */
    DocumentWriter addProperty( String name,
                                Object[] values );

    /**
     * Adds a multi-value property with the given name to the underlying document's properties.
     * 
     * @param name the name of the property; may not be null
     * @param values the values for the property; may not be null but may be empty
     * @return this writer; never null
     */
    DocumentWriter addProperty( Name name,
                                Object[] values );

    /**
     * Adds a multi-value property with the given name to the underlying document's properties.
     * 
     * @param name the name of the property; may not be null
     * @param firstValue the first value for the property; may not be null
     * @param additionalValues additional values for the property
     * @return this writer; never null
     */
    DocumentWriter addProperty( String name,
                                Object firstValue,
                                Object... additionalValues );

    /**
     * Adds a multi-value property with the given name to the underlying document's properties.
     * 
     * @param name the name of the property; may not be null
     * @param firstValue the first value for the property; may not be null
     * @param additionalValues additional values for the property
     * @return this writer; never null
     */
    DocumentWriter addProperty( Name name,
                                Object firstValue,
                                Object... additionalValues );

    /**
     * Adds a map of properties to the the underlying document's properties.
     * 
     * @param properties a map of properties keyed by their name; may not be null
     * @return this writer; never null
     */
    DocumentWriter addProperties( Map<Name, Property> properties );

    /**
     * Adds an additional value to an existing property with the given name. If such a property does not yet exist, this will
     * create the property.
     * 
     * @param name the name of the property; may not be null
     * @param value the new value that should be added to the property; may not be null
     * @return this writer; never null
     */
    DocumentWriter addPropertyValue( String name,
                                     Object value );

    /**
     * Adds an additional value to an existing property with the given name. If such a property does not yet exist, this will
     * create the property.
     * 
     * @param name the name of the property; may not be null
     * @param value the new value that should be added to the property; may not be null
     * @return this writer; never null
     */
    DocumentWriter addPropertyValue( Name name,
                                     Object value );

    /**
     * Replaces the underlying document's properties with the one from the given map.
     * 
     * @param properties a map of properties keyed by their name; may not be null
     * @return this writer; never null
     */
    DocumentWriter setProperties( Map<Name, Property> properties );

    /**
     * Sets the ids of one or more parents on the underlying document. If any parents previously existed, they will be replaced.
     * 
     * @param parentIds the array of parent identifiers
     * @return this writer; never null
     */
    DocumentWriter setParents( String... parentIds );

    /**
     * Sets the id of the parent of the underlying document. If any parents previously existed, they will be replaced.
     * 
     * @param parentId the identifier of the parent; may not be null
     * @return this writer; never null
     */
    DocumentWriter setParent( String parentId );

    /**
     * Sets the ids of one or more parents on the underlying document. If any parents previously existed, they will be replaced.
     * 
     * @param parentIds the list of parent identifiers; may not be null
     * @return this writer; never null
     */
    DocumentWriter setParents( List<String> parentIds );

    /**
     * Sets the value, in seconds, of the amount of the time the underlying document should be cached.
     * 
     * @param seconds the number of seconds the document should be cached by the repository.
     * @return this writer; never null
     */
    DocumentWriter setCacheTtlSeconds( int seconds );

    /**
     * Sets a flag on the underlying document which indicates that it should not be indexed (and therefore will not appear
     * in queries) by the repository.
     *
     * @return this writer; never null;
     */
    DocumentWriter setNotQueryable();

    @Override
    DocumentWriter addChild( String id,
                             String name );

    @Override
    DocumentWriter addChild( String id,
                             Name name );

    @Override
    DocumentWriter setChildren( List<? extends Document> children );

    @Override
    DocumentWriter setChildren( LinkedHashMap<String, Name> children );

    @Override
    DocumentWriter addPage( String parentId,
                            int nextPageOffset,
                            long blockSize,
                            long totalChildCount );

    @Override
    DocumentWriter addPage( String parentId,
                            String nextPageOffset,
                            long blockSize,
                            long totalChildCount );

    /**
     * Some connectors may want to pre-generate additional documents when {@link Connector#getDocumentById(String)} is called. In
     * such a case, the connector can use this method to obtain a writer for an additional document and use it in much the same
     * was as {@link Connector#newDocument(String)}. The resulting additional document will be included automatically when the
     * Connector returns the {@link #document() top-level document}.
     * 
     * @param id the identifier of the additional document; may not be null
     * @return the writer for the additional document; never null
     */
    DocumentWriter writeAdditionalDocument( String id );

}
