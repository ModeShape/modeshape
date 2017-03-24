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
package org.modeshape.jcr.locking;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.util.CheckArg;
import org.modeshape.schematic.Lockable;

/**
 * {@link LockingService} implementation which uses DB locking, via a {@link Lockable} instance.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.2
 */
public class DbLockingService implements LockingService {
    
    private final Lockable db;
    private final long lockTimeoutMillis;

    /**
     * Creates a new db locking service instance.
     *
     * @param lockTimeoutMillis the number of milliseconds to wait by default for a lock to be obtained, before timing out
     * @param db a {@link Lockable} instance; never {@code null}
     */
    public DbLockingService(long lockTimeoutMillis, Lockable db) {
        CheckArg.isNonNegative(lockTimeoutMillis, "lockTimeout");
        this.lockTimeoutMillis = lockTimeoutMillis;
        this.db = db;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit, String... names) throws InterruptedException {
        long start = System.currentTimeMillis();
        boolean result;
        long timeInMills = TimeUnit.MILLISECONDS.convert(time, unit);
        while (!(result = db.lockForWriting(names)) && 
               System.currentTimeMillis() - start <= timeInMills) {
            //wait a bit (between 50 and 300 ms)
            long sleepDurationMillis = 50 + ThreadLocalRandom.current().nextInt(251);
            Thread.sleep(sleepDurationMillis);
        }
        return result;        
    }

    @Override
    public boolean tryLock(String... names) throws InterruptedException {
        return tryLock(lockTimeoutMillis, TimeUnit.MILLISECONDS, names);
    }

    @Override
    public boolean unlock(String... names) {
        // the DB should automatically release locks at the end of each transaction
        return true;
    }

    @Override
    public boolean shutdown() {
        // nothing to shutdown by default...
        return true;
    }
}
