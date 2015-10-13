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

package org.modeshape.jcr.index.local;

import javax.jcr.query.qom.StaticOperand;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.Fun;
import org.mapdb.Serializer;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.spi.index.Index;

/**
 * An {@link Index} that associates a single value with a single {@link NodeKey}. All values are stored in natural sort order,
 * allowing finding all the node keys for a range of values.
 *
 * @param <T> the type of values
 * @author Randall Hauch (rhauch@redhat.com)
 */
class LocalUniqueIndex<T> extends LocalMapIndex<T, T> {

    /**
     * Create a new index that allows only a single value for each unique key.
     *
     * @param name the name of the index; may not be null or empty
     * @param workspaceName the name of the workspace; may not be null
     * @param db the database in which the index information is to be stored; may not be null
     * @param converter the converter from {@link StaticOperand} to values being indexed; may not be null
     * @param valueSerializer the serializer for the type of value being indexed; may not be null
     * @param rawSerializer the raw value serializer for the type of value being indexed; may not be null
     * @return the new index; never null
     */
    static <T> LocalUniqueIndex<T> create( String name,
                                           String workspaceName,
                                           DB db,
                                           Converter<T> converter,
                                           BTreeKeySerializer<T> valueSerializer,
                                           Serializer<T> rawSerializer ) {
        return new LocalUniqueIndex<>(name, workspaceName, db, converter, valueSerializer, rawSerializer);
    }

    /**
     * Create a new unique index.
     *
     * @param name the name of the index; may not be null or empty
     * @param workspaceName the name of the workspace; may not be null
     * @param db the database in which the index information is to be stored; may not be null
     * @param converter the converter from {@link StaticOperand} to values being indexed; may not be null
     * @param valueSerializer the serializer for the type of value being indexed; may not be null
     * @param rawSerializer the raw value serializer for the type of value being indexed; may not be null
     */
    protected LocalUniqueIndex( String name,
                                String workspaceName,
                                DB db,
                                Converter<T> converter,
                                BTreeKeySerializer<T> valueSerializer,
                                Serializer<T> rawSerializer ) {
        super(name, workspaceName, db, converter, valueSerializer, rawSerializer);

    }

    @Override
    public void add( String nodeKey,
                     String propertyName, 
                     T value ) {
        logger.trace("Adding node '{0}' to '{1}' index with value '{2}'", nodeKey, name, value);
        keysByValue.put(value, nodeKey);
    }

    @Override
    public void remove( String nodeKey,
                        String propertyName, 
                        T value ) {
        // Find all of the T values (entry keys) for the given node key (entry values) ...
        for (T key : Fun.filter(valuesByKey, nodeKey)) {
            if (comparator.compare(value, key) == 0) {
                logger.trace("Removing node '{0}' from '{1}' index with value '{2}'", nodeKey, name, value);
                keysByValue.remove(key);
            }
        }
    }

    @Override
    public void remove( String nodeKey ) {
        // Find all of the T values (entry keys) for the given node key (entry values) ...
        for (T key : Fun.filter(valuesByKey, nodeKey)) {
            logger.trace("Removing node '{0}' from '{1}' index with value '{2}'", nodeKey, name, key);
            keysByValue.remove(key);
        }
    }

}
