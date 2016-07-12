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

import java.util.concurrent.TimeUnit;
import org.modeshape.schematic.Lockable;

/**
 * {@link LockingService} implementation which uses DB locking, via a {@link Lockable} instance.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.2
 */
public class DbLockingService implements LockingService {
    
    private final JGroupsLockingService jgroupsLockingService;
    private final String initializationLockName;
    private final Lockable db;

    /**
     * Creates a new db locking service instance.
     * 
     * @param jgroupsLockingService the {@link JGroupsLockingService} instance to which some locking may be delegated; never {@code null}
     * @param initializationLockName the name of the lock used for cluster-wide initialization, which cannot be handled by this 
     * service; never {@code null}
     * @param db a {@link Lockable} instance; never {@code null}
     */
    public DbLockingService(JGroupsLockingService jgroupsLockingService, String initializationLockName, Lockable db) {
        this.jgroupsLockingService = jgroupsLockingService;
        this.initializationLockName = initializationLockName;
        this.db = db;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit, String... names) throws InterruptedException {
        if (isInitializationLock(names)) {
            // the db cannot handle the initialization lock, so delegate to JG
            return jgroupsLockingService.tryLock(time, unit, names[0]);
        }
        
        long start = System.nanoTime();
        boolean result = false;
        while (!(result = db.lockForWriting(names)) && 
               TimeUnit.MILLISECONDS.convert((System.nanoTime() - start), TimeUnit.NANOSECONDS) < time) {
            //wait a bit
            Thread.sleep(1000);
        }
        return result;        
    }

    private boolean isInitializationLock(String... names) {
        return names.length == 1 && names[0].equals(initializationLockName);
    }

    @Override
    public boolean tryLock(String... names) throws InterruptedException {
        return tryLock(jgroupsLockingService.getLockTimeoutMillis(), TimeUnit.MILLISECONDS, names);
    }

    @Override
    public boolean unlock(String... names) {
        if (isInitializationLock(names)) {
            return jgroupsLockingService.unlock(names);
        }
        // the DB should automatically release locks at the end of each transaction
        return true;
    }

    @Override
    public boolean shutdown() {
        return jgroupsLockingService.shutdown();
    }
}
