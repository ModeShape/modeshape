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
public interface DocumentWriter extends PagingWriter {

    /**
     * Sets an identifier string on the underlying document.
     *
     * @param id a {@code non-null} String
     * @return this writer's instance
     */
    public DocumentWriter setId( String id );

    /**
     * Sets the primary type of the underlying document.
     *
     * @param name a {@code non-null} String representing a valid type name.
     * @return this writer's instance
     */
    public DocumentWriter setPrimaryType( String name );

    /**
     * Sets the primary type of the underlying document.
     *
     * @param name a {@code non-null} {@link Name} representing a valid type name.
     * @return this writer's instance
     */
    public DocumentWriter setPrimaryType( Name name );

    /**
     * Adds a mixin type to the underlying document.
     *
     * @param name a {@code non-null} {@link String} representing a valid type name.
     * @return this writer's instance
     */
    public DocumentWriter addMixinType( String name );

    /**
     * Adds a mixin type to the underlying document.
     *
     * @param name a {@code non-null} {@link Name} representing a valid type name.
     * @return this writer's instance
     */
    public DocumentWriter addMixinType( Name name );

    /**
     * Adds a property with the given name and value to the underlying document's properties.
     *
     * @param name a {@code non-null} {@link String} representing the name of the property
     * @param value a {@code non-null} {@link Object} representing the value for the property
     * @return this writer's instance
     */
    public DocumentWriter addProperty( String name,
                                       Object value );

    /**
     * Adds a property with the given name and value to the underlying document's properties.
     *
     * @param name a {@code non-null} {@link Name} representing the name of the property
     * @param value a {@code non-null} {@link Object} representing the value for the property
     * @return this writer's instance
     */
    public DocumentWriter addProperty( Name name,
                                       Object value );

    /**
     * Adds a multi-value property with the given name to the underlying document's properties.
     *
     * @param name a {@code non-null} {@link String} representing the name of the property
     * @param values a {@code non-null} {@link Object[]} representing the value for the property
     * @return this writer's instance
     */
    public DocumentWriter addProperty( String name,
                                       Object[] values );

    /**
     * Adds a multi-value property with the given name to the underlying document's properties.
     *
     * @param name a {@code non-null} {@link Name} representing the name of the property
     * @param values a {@code non-null} {@link Object[]} representing the value for the property
     * @return this writer's instance
     */
    public DocumentWriter addProperty( Name name,
                                       Object[] values );

    /**
     * Adds a multi-value property with the given name to the underlying document's properties.
     *
     * @param name a {@code non-null} {@link String} representing the name of the property
     * @param firstValue a {@code non-null} {@link Object} representing the first value for the property
     * @param additionalValues an optional {@link Object[]} representing additional values.
     * @return this writer's instance
     */
    public DocumentWriter addProperty( String name,
                                       Object firstValue,
                                       Object... additionalValues );

    /**
     * Adds a multi-value property with the given name to the underlying document's properties.
     *
     * @param name a {@code non-null} {@link Name} representing the name of the property
     * @param firstValue a {@code non-null} {@link Object} representing the first value for the property
     * @param additionalValues an optional {@link Object[]} representing additional values.
     * @return this writer's instance
     */
    public DocumentWriter addProperty( Name name,
                                       Object firstValue,
                                       Object... additionalValues );

    /**
     * Adds a map of properties to the the underlying document's properties.
     *
     * @param properties a {@code non-null} {@link Map} representing a map of properties
     * @return this writer's instance
     */
    public DocumentWriter addProperties( Map<Name, Property> properties );

    /**
     * Adds an additional value to an existing property with the given name. If such a property does not yet exist, this will
     * create the property.
     *
     * @param name a {@code non-null} {@link String} representing the name of the property
     * @param value a {@code non-null} {@link Object} representing the value for the property
     * @return this writer's instance
     */
    public DocumentWriter addPropertyValue( String name,
                                            Object value );

    /**
     * Adds an additional value to an existing property with the given name. If such a property does not yet exist, this will
     * create the property.
     *
     * @param name a {@code non-null} {@link Name} representing the name of the property
     * @param value a {@code non-null} {@link Object} representing the value for the property
     * @return this writer's instance
     */
    public DocumentWriter addPropertyValue( Name name,
                                            Object value );

    /**
     * Adds a child with the given id and name to the underlying document.
     *
     * @param id a {@code non-null} String representing the new child's id.
     * @param name a {@code non-null} String representing the new child's name.
     * @return this writer's instance
     */
    public DocumentWriter addChild( String id,
                                    String name );

    /**
     * Adds a child with the given id and name to the underlying document.
     *
     * @param id a {@code non-null} String representing the new child's id.
     * @param name a {@code non-null} {@code Name} representing the new child's name.
     * @return this writer's instance
     */
    public DocumentWriter addChild( String id,
                                    Name name );

    /**
     * Replaces the underlying document's properties with the one from the given map.
     * @param properties {@code non-null} {@link Map} representing a map of properties
     * @return this writer's instance
     */
    public DocumentWriter setProperties( Map<Name, Property> properties );

    /**
     * Sets the ids of one or more parents on the underlying document. If any parents previously existed, they will be replaced.
     *
     * @param parentIds an {@code non-null} {@link String[]}
     * @return this writer's instance
     */
    public DocumentWriter setParents( String... parentIds );

    /**
     * Sets the id of the parent of the underlying document. If any parents previously existed, they will be replaced.
     *
     * @param parentId an {@code non-null} {@link String}
     * @return this writer's instance
     */
    public DocumentWriter setParent( String parentId );

    /**
     * Sets the ids of one or more parents on the underlying document. If any parents previously existed, they will be replaced.
     *
     * @param parentIds an {@code non-null} {@link List}
     * @return this writer's instance
     */
    public DocumentWriter setParents( List<String> parentIds );

    /**
     * Returns the underlying document.
     *
     * @return a {@code non-null} {@link EditableDocument} instance.
     */
    public EditableDocument document();

    /**
     * Sets the list of children for the underlying document. If children previously existed, they will be replaced.
     *
     * @param children a {@code non-null} {@link List} of {@link EditableDocument}
     * @return this writer's instance
     */
    public DocumentWriter setChildren( List<? extends Document> children );

    /**
     * Sets the value, in seconds, of the amount of the time the underlying document should be cached.
     *
     * @param seconds the number of seconds the document should be cached by the repostiry.
     * @return this writer's instance
     */
    public DocumentWriter setCacheTtlSeconds( int seconds );

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

}
