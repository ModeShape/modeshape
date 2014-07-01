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

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import javax.jcr.query.qom.StaticOperand;
import org.mapdb.DB;
import org.mapdb.Serializer;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.index.local.MapDB.UniqueKey;
import org.modeshape.jcr.spi.index.Index;

/**
 * An {@link Index} that associates a single value with multiple {@link NodeKey}s. All values are stored in natural sort order,
 * allowing finding all the node keys for a range of values.
 * 
 * @param <T> the type of values
 * @author Randall Hauch (rhauch@redhat.com)
 */
final class LocalDuplicateIndex<T> extends LocalMapIndex<UniqueKey<T>, T> {

    /**
     * Create a new index that allows duplicate values across all keys.
     * 
     * @param name the name of the index; may not be null or empty
     * @param workspaceName the name of the workspace; may not be null
     * @param providerName the name of the provider; may not be null
     * @param db the database in which the index information is to be stored; may not be null
     * @param converter the converter from {@link StaticOperand} to values being indexed; may not be null
     * @param valueSerializer the serializer for the type of value being indexed
     * @param comparator the comparator for the values; may not be null
     * @return the new index; never null
     */
    static <T> LocalDuplicateIndex<T> create( String name,
                                              String workspaceName,
                                              String providerName,
                                              DB db,
                                              Converter<T> converter,
                                              Serializer<T> valueSerializer,
                                              Comparator<T> comparator ) {
        return new LocalDuplicateIndex<>(name, workspaceName, providerName, db, converter, valueSerializer, comparator);
    }

    private static final String NEXT_COUNTER = "next-counter";

    private final AtomicLong counter;

    protected LocalDuplicateIndex( String name,
                                   String workspaceName,
                                   String providerName,
                                   DB db,
                                   Converter<T> converter,
                                   Serializer<T> valueSerializer,
                                   Comparator<T> comparator ) {
        super(name, workspaceName, providerName, db, IndexValues.uniqueKeyConverter(converter),
              MapDB.uniqueKeySerializer(valueSerializer, comparator));
        Long nextCounter = (Long)options.get(NEXT_COUNTER);
        this.counter = new AtomicLong(nextCounter != null ? nextCounter.longValue() : 0L);
    }

    @Override
    public void close() {
        // Store the value of the next counter in the options map ...
        options.put(NEXT_COUNTER, counter.get());
        super.close();
    }

    @Override
    public void add( String nodeKey,
                     T value ) {
        keysByValue.put(new UniqueKey<T>(value, counter.getAndIncrement()), nodeKey);
    }

}
