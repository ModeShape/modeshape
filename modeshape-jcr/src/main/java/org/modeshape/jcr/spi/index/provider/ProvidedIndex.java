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
package org.modeshape.jcr.spi.index.provider;

/**
 * Interface that should be implemented by different index providers which provide specific index {@link org.modeshape.jcr.api.index.IndexDefinition.IndexKind}
 * to the repository. 
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @param <T> the type of the values handled by this index or {@link Object} in the case of multi-column indexes.
 * @since 4.5
 */
public interface ProvidedIndex<T> extends Filter, Costable, Reindexable, Lifecycle {

    /**
     * Adds a single value to this index for the given node.
     *
     * @param nodeKey a {@link org.modeshape.jcr.cache.NodeKey} instance, never {@code null}
     * @param propertyName the name for which the value is removed, never {@code null} but certain providers
     * may ignore the value. Most of the times this is the real JCR property name, but for some index types (e.g. node types
     * index) this may be something else (e.g. the name of the index)
     * @param value an Object instance, never {@code null}
     */
    void add( String nodeKey, String propertyName, T value );

    /**
     * Adds multiple values to the index for the given node.
     *
     * @param nodeKey a {@link org.modeshape.jcr.cache.NodeKey} instance, never {@code null}
     * @param propertyName the name for which the value is removed, never {@code null} but certain providers
     * may ignore the value. Most of the times this is the real JCR property name, but for some index types (e.g. node types
     * index) this may be something else (e.g. the name of the index)
     * @param values an array of values, never {@code null} or empty
     */
    void add( String nodeKey, 
              String propertyName, 
              T[] values );

    /**
     * Removes the given node from the index.
     * 
     * @param nodeKey a {@link org.modeshape.jcr.cache.NodeKey} instance, never {@code null}
     */
    void remove( String nodeKey );

    /**
     * Removes a value for the given node from the index.
     *
     * @param nodeKey a {@link org.modeshape.jcr.cache.NodeKey} instance, never {@code null}
     * @param propertyName the name for which the value is removed, never {@code null} but certain providers
     * may ignore the value. Most of the times this is the real JCR property name, but for some index types (e.g. node types
     * index) this may be something else (e.g. the name of the index)
     * @param value the value to remove from the index for this node, never {@code null}
     */
    void remove( String nodeKey,
                 String propertyName, 
                 T value );

    /**
     * Removes multiple values from the index for the given node.
     *
     * @param nodeKey a {@link org.modeshape.jcr.cache.NodeKey} instance, never {@code null}
     * @param propertyName the name for which the value is removed, never {@code null} but certain providers
     * may ignore the value. Most of the times this is the real JCR property name, but for some index types (e.g. node types
     * index) this may be something else (e.g. the name of the index)
     * @param values the values to remove, never {@code null} or empty
     */
    void remove( String nodeKey, String propertyName, T[] values );

    /**
     * Commits any potential changes made by the add/remove/update operations to this index.
      */
    void commit();

    /**
     * Get the name of the index.
     *
     * @return the index name; never null
     */
    String getName();
}
