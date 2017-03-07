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

import java.util.concurrent.locks.Lock;
import org.jgroups.Channel;
import org.jgroups.blocks.locking.LockService;
import org.jgroups.protocols.CENTRAL_LOCK;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.modeshape.common.annotation.ThreadSafe;

/**
 * Locking service which should be used by ModeShape when running in a cluster
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 * @deprecated this is not reliable when running in a cluster and will be removed in the next major version.
 */
@ThreadSafe
@Deprecated
public class JGroupsLockingService extends AbstractLockingService<Lock> {

    /**
     * The service used for cluster-wide locking
     */
    private final LockService lockService;

    /**
     * Creates a new service instance using the supplied JGroups channel.
     * <p>
     * Note that the channel is expected to have been initialized and the CENTRAL_LOCK protocol added.
     * </p>
     *
     * @param channel a {@link Channel} instance; may not be null
     * @param lockTimeoutMillis the number of millis to wait before timing out when attempting to obtain a lock
     */
    public JGroupsLockingService(Channel channel, long lockTimeoutMillis) {
        super(lockTimeoutMillis);
        
        ProtocolStack protocolStack = channel.getProtocolStack();
        Protocol centralLock = protocolStack.findProtocol(CENTRAL_LOCK.class);
        if (centralLock == null) {
            throw new IllegalArgumentException("JGroups protocol stack does not contain a CENTRAL_LOCK protocol...");
        }
        this.lockService = new LockService(channel);
    }

    @Override
    protected Lock createLock(String name) {
        return lockService.getLock(name);
    }
    
    @Override
    protected boolean releaseLock(Lock lock) {
        // the only way to tell if JG has a lock is to try locking it from the current thread (which should be a no-op if the lock 
        // is already held...
        if (lock.tryLock()) {
            lock.unlock();
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void doShutdown() {
        lockService.unlockAll();
    }
}
