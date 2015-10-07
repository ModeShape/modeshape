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

import org.mapdb.DB;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.spi.index.provider.ProvidedIndex;

/**
 * Base class for all local index types.
 * 
 * @param <T> the type of value that is added to the index
 * @author Randall Hauch (rhauch@redhat.com)
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class LocalIndex<T> implements ProvidedIndex<T> {

    protected final Logger logger = Logger.getLogger(getClass());

    protected final String name;
    protected final String workspace;
    protected final IndexUpdater indexUpdater;
    protected final DB db;

    protected LocalIndex( String name, String workspace, DB db ) {
        assert name != null;
        assert workspace != null;
        
        this.name = name;
        this.workspace = workspace;
        this.db = db;
        this.indexUpdater = new IndexUpdater(db);
    }

    @Override
    public void add( String nodeKey, String propertyName, T[] values ) {
        for (T value : values) {
            add(nodeKey, propertyName, value);
        }
    }
    
    @Override
    public void remove( String nodeKey, String propertyName, T[] values ) {
        for (T value : values) {
            remove(nodeKey, propertyName, value);
        }
    }

    @Override
    public void commit() {
        indexUpdater.commit();
    }
}
