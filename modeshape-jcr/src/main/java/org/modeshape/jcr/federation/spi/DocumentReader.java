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
import java.util.Set;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * A reader which can be used read the structure of {@link Document} instances and expose the underlying information.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface DocumentReader {
    /**
     * Returns the id of the underlying document.
     *
     * @return a {@code non-null} {@link String}
     */
    public String getDocumentId();

    /**
     * Returns the list of identifiers of the parents of the underlying document.
     *
     * @return a {@code non-null} {@link List}
     */
    public List<String> getParentIds();

    /**
     * Returns the list of children the underlying document has.
     *
     * @return a {@code non-null} {@link List}
     */
    public List<? extends Document> getChildren();

    /**
     * Returns the underlying document instance.
     *
     * @return a {@code non-null} {@link Document} instance.
     */
    public Document document();

    /**
     * Returns the number of seconds that the underlying document should be cached by the repository, if any.
     *
     * @return either an optional {@link Integer}. If {@code null}, it means that there isn't a special requirement for the
     * underlying document.
     */
    public Integer getCacheTtlSeconds();

    /**
     * Returns name of the primary type of the underlying document.
     *
     * @return a {@code non-null} {@link Name}
     */
    public Name getPrimaryType();

    /**
     * Returns the name of primary type of the underlying document.
     *
     * @return a {@code non-null} {@link String}
     */
    public String getPrimaryTypeName();

    /**
     * Returns a set with the names of the primary types of the underlying document.
     *
     * @return a {@code non-null} {@link Set}
     */
    public Set<Name> getMixinTypes();

    /**
     * Returns a set with the names of the primary types of the underlying document.
     *
     * @return a {@code non-null} {@link Set}
     */
    public Set<String> getMixinTypeNames();

    /**
     * Returns the property which has the given name, or null if there is no such property.
     *
     * @param name a {@code non-null} {@link Name}
     * @return either a {@link Property} instance or {@code null}
     */
    public Property getProperty( Name name );

    /**
     * Returns the property which has the given name, or null if there is no such property.
     *
     * @param name a {@code non-null} {@link Name}
     * @return either a {@link Property} instance or {@code null}
     */
    public Property getProperty( String name );

    /**
     * Returns all the properties of the underlying document.
     *
     * @return a {@code non-null} {@link Map} of (property name, property value) pairs.
     */
    public Map<Name, Property> getProperties();
}
