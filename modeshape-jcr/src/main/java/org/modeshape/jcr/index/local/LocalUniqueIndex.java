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
     * @param providerName the name of the provider; may not be null
     * @param db the database in which the index information is to be stored; may not be null
     * @param converter the converter from {@link StaticOperand} to values being indexed; may not be null
     * @param valueSerializer the serializer for the type of value being indexed
     * @return the new index; never null
     */
    static <T> LocalUniqueIndex<T> create( String name,
                                           String workspaceName,
                                           String providerName,
                                           DB db,
                                           Converter<T> converter,
                                           BTreeKeySerializer<T> valueSerializer ) {
        return new LocalUniqueIndex<>(name, workspaceName, providerName, db, converter, valueSerializer);
    }

    /**
     * Create a new unique index.
     * 
     * @param name the name of the index; may not be null or empty
     * @param workspaceName the name of the workspace; may not be null
     * @param providerName the name of the provider; may not be null
     * @param db the database in which the index information is to be stored; may not be null
     * @param converter the converter from {@link StaticOperand} to values being indexed; may not be null
     * @param valueSerializer the serializer for the type of value being indexed
     */
    protected LocalUniqueIndex( String name,
                                String workspaceName,
                                String providerName,
                                DB db,
                                Converter<T> converter,
                                BTreeKeySerializer<T> valueSerializer ) {
        super(name, workspaceName, providerName, db, converter, valueSerializer);

    }

    @Override
    public void add( String nodeKey,
                     T value ) {
        keysByValue.put(value, nodeKey);
    }

}
