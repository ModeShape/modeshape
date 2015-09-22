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

import org.mapdb.Atomic;
import org.mapdb.DB;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * Class which handles various aspects related to local index updating in terms of writing data to MapDB.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Immutable
@ThreadSafe
public final class IndexUpdater {
    private static final String LAST_SUCCESSFUL_UPDATE = "$lastSuccessfulUpdateAt";
    
    private final DB db;

    protected IndexUpdater( DB db ) {
        this.db = db;
    }
    
    protected void commit() {
        writeLatestUpdateTime(System.currentTimeMillis());
        db.commit();
    }

    protected Long latestIndexUpdateTime() {
        // note that if the field doest not exist, this will return 0
        return db.getAtomicLong(LAST_SUCCESSFUL_UPDATE).get();
    }

    private void writeLatestUpdateTime( long updateTime ) {
        Atomic.Long atomicLong = db.getAtomicLong(LAST_SUCCESSFUL_UPDATE);
        long latestValue = atomicLong.get();
        if (latestValue < updateTime) {
            atomicLong.compareAndSet(latestValue, updateTime);
        }
    }
}
