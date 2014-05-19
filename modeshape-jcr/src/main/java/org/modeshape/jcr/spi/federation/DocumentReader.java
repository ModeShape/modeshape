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

package org.modeshape.jcr.spi.federation;

import java.util.LinkedHashMap;
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
    String getDocumentId();

    /**
     * Returns the list of identifiers of the parents of the underlying document.
     * 
     * @return a {@code non-null} {@link List}
     */
    List<String> getParentIds();

    /**
     * Returns the list of children the underlying document has.
     * 
     * @return a {@code non-null} {@link List}
     */
    List<? extends Document> getChildren();

    /**
     * Returns an ordered map of (childId, childName) the underlying document has. This preserves the order of the children as
     * defined on the document.
     * <p>
     * The returned map does not contain any explicit same name sibling information, so a connector interested in that would have
     * to do its own, custom processing.
     * </p>
     * 
     * @return a {@code non-null} {@link LinkedHashMap}
     */
    LinkedHashMap<String, Name> getChildrenMap();

    /**
     * Returns the underlying document instance.
     * 
     * @return a {@code non-null} {@link Document} instance.
     */
    Document document();

    /**
     * Returns the number of seconds that the underlying document should be cached by the repository, if any.
     * 
     * @return either an optional {@link Integer}. If {@code null}, it means that there isn't a special requirement for the
     *         underlying document.
     */
    Integer getCacheTtlSeconds();

    /**
     * Returns name of the primary type of the underlying document.
     * 
     * @return a {@code non-null} {@link Name}
     */
    Name getPrimaryType();

    /**
     * Returns the name of primary type of the underlying document.
     * 
     * @return a {@code non-null} {@link String}
     */
    String getPrimaryTypeName();

    /**
     * Returns a set with the names of the primary types of the underlying document.
     * 
     * @return a {@code non-null} {@link Set}
     */
    Set<Name> getMixinTypes();

    /**
     * Returns a set with the names of the primary types of the underlying document.
     * 
     * @return a {@code non-null} {@link Set}
     */
    Set<String> getMixinTypeNames();

    /**
     * Returns the property which has the given name, or null if there is no such property.
     * 
     * @param name a {@code non-null} {@link Name}
     * @return either a {@link Property} instance or {@code null}
     */
    Property getProperty( Name name );

    /**
     * Returns the property which has the given name, or null if there is no such property.
     * 
     * @param name a {@code non-null} {@link Name}
     * @return either a {@link Property} instance or {@code null}
     */
    Property getProperty( String name );

    /**
     * Returns all the properties of the underlying document.
     * 
     * @return a {@code non-null} {@link Map} of (property name, property value) pairs.
     */
    Map<Name, Property> getProperties();

    /**
     * Determine whether the document is considered queryable.
     * 
     * @return true if the document is queryable, or false if it is not
     */
    boolean isQueryable();

}
