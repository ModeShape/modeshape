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
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentMap;
import javax.jcr.query.qom.Constraint;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.Bind;
import org.mapdb.DB;
import org.mapdb.Fun;
import org.mapdb.Serializer;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.value.ValueComparators;

/**
 * Abstract map-based index.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 * @param <T> the type of value to be indexed
 * @param <V> the raw type of value to be added
 */
abstract class LocalMapIndex<T, V> extends LocalIndex<V> {

    protected final BTreeMap<T, String> keysByValue;
    protected final NavigableSet<Fun.Tuple2<String, T>> valuesByKey;
    protected final ConcurrentMap<String, Object> options;
    private final Converter<T> converter;
   
    protected final Comparator<T> comparator;
    private final boolean isNew;

    LocalMapIndex( String name,
                   String workspaceName,
                   DB db,
                   Converter<T> converter,
                   BTreeKeySerializer<T> valueSerializer,
                   Serializer<T> valueRawSerializer ) {
        super(name, workspaceName, db);

        assert converter != null;
        assert valueSerializer != null;
        this.converter = converter;
        if (db.exists(name)) {
            logger.debug("Reopening storage for '{0}' index in workspace '{1}'", name, workspaceName);
            this.options = db.getHashMap(name + "/options");
            this.keysByValue = db.getTreeMap(name);
            this.valuesByKey = db.getTreeSet(name + "/inverse");
            this.isNew = false;
        } else {
            logger.debug("Creating storage for '{0}' index in workspace '{1}'", name, workspaceName);
            this.isNew = true;
            this.options = db.createHashMap(name + "/options").make();
            this.keysByValue = db.createTreeMap(name).counterEnable().comparator(valueSerializer.getComparator())
                                 .keySerializer(valueSerializer).make();
            // Create the TreeSet used in the reverse mapping, but we have to set a comparator that works in terms of the
            // Fun.Tuple2<String,T> ...
            final Comparator<String> strComparator = ValueComparators.STRING_COMPARATOR;
            final Serializer<String> strSerializer = Serializer.STRING;
            final Comparator<T> valueComparator = valueSerializer.getComparator();
            final Comparator<Fun.Tuple2<String, T>> revComparator = MapDB.tupleComparator(strComparator, valueComparator);
            final BTreeKeySerializer<Fun.Tuple2<String, T>> revSerializer = MapDB.tupleBTreeSerializer(strComparator,
                                                                                                       strSerializer,
                                                                                                       valueRawSerializer,
                                                                                                       revComparator);
            this.valuesByKey = db.createTreeSet(name + "/inverse").comparator(revComparator).serializer(revSerializer).make();
        }
        this.comparator = valueSerializer.getComparator();

        // Bind the map and the set together so the set is auto-updated as the map is changed ...
        Bind.mapInverse(this.keysByValue, this.valuesByKey);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getWorkspaceName() {
        return workspace;
    }

    @Override
    public boolean requiresReindexing() {
        return isNew;
    }

    @Override
    public long estimateTotalCount() {
        return keysByValue.sizeLong();
    }

    protected final Converter<T> converter() {
        return converter;
    }

    @Override
    public Results filter( IndexConstraints filter ) {
        return Operations.createFilter(keysByValue, converter, filter.getConstraints(), filter.getVariables()).getResults();
    }

    @Override
    public long estimateCardinality( List<Constraint> andedConstraints,
                                     Map<String, Object> variables ) {
        return Operations.createFilter(keysByValue, converter, andedConstraints, variables).estimateCount();
    }

    @Override
    public void clearAllData() {
        keysByValue.clear();
    }

    @Override
    public void shutdown( boolean destroyed ) {
        if (destroyed) {
            // Remove the database since the index was destroyed ...
            db.delete(name);
        }
    }

}
