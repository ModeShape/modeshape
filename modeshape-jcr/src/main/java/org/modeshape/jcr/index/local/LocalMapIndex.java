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

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentMap;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.Bind;
import org.mapdb.DB;
import org.mapdb.DB.BTreeMapMaker;
import org.mapdb.Fun;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.spi.index.IndexConstraints;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 * @param <T> the type of value to be indexed
 * @param <V> the raw type of value to be added
 */
abstract class LocalMapIndex<T, V> implements LocalIndex<V> {

    private final String name;
    private final String workspace;
    private final String providerName;
    protected final BTreeMap<T, String> keysByValue;
    protected final NavigableSet<Fun.Tuple2<String, T>> valuesByKey;
    protected final ConcurrentMap<String, Object> options;
    private final Converter<T> converter;

    LocalMapIndex( String name,
                   String workspaceName,
                   String providerName,
                   DB db,
                   Converter<T> converter,
                   BTreeKeySerializer<T> valueSerializer ) {
        this.name = name;
        this.workspace = workspaceName;
        this.providerName = providerName;
        this.converter = converter;
        if (db.exists(name)) {
            this.keysByValue = db.getTreeMap(name);
            this.valuesByKey = db.getTreeSet(name + "/inverse");
            this.options = db.getHashMap(name + "/options");
        } else {
            BTreeMapMaker maker = db.createTreeMap(name).counterEnable();
            if (valueSerializer != null) maker.keySerializer(valueSerializer);
            this.keysByValue = maker.make();
            this.valuesByKey = db.createTreeSet(name + "/inverse").make();
            this.options = db.createHashMap(name + "/options").make();
        }

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
    public String getProviderName() {
        return providerName;
    }

    @Override
    public boolean supportsFullTextConstraints() {
        return false;
    }

    protected final Converter<T> converter() {
        return converter;
    }

    @Override
    public Results filter( IndexConstraints filter ) {
        return Operations.createOperation(keysByValue, converter, filter.getConstraints());
    }

    @Override
    public void remove( String nodeKey ) {
        // Final all of the T values (entry keys) for the given node key (entry values) ...
        for (T key : Fun.filter(valuesByKey, nodeKey)) {
            keysByValue.remove(key);
        }
    }

    @Override
    public void close() {
        // do nothing by default ...
    }

}
