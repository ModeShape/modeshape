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
package org.modeshape.jcr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import javax.jcr.Node;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for garbage collection process related to lock cleanup.
 * 
 * @author Illia Khokholkov
 *
 */
public class LockCleanupTest extends MultiUseAbstractTest {

    @BeforeClass
    public static void beforeAll() throws Exception {
        startRepository(RepositoryConfiguration.read(LockCleanupTest.class
                .getResource("/config/repo-config-garbage-collection.json")));
    }
    
    /**
     * Checks scheduled lock cleanup. The repository is configured to start lock cleanup process
     * right away, i.e. no initial delay, and run it every minute. The node used for testing is
     * locked for one second. After approximately a minute, the node should be automatically
     * unlocked by the background garbage collection process.
     * 
     * @throws Exception
     *             if an error occurred
     */
    @Test
    public void checkScheduledLockCleanup() throws Exception {
        Node node = session.getRootNode().addNode("toLock");
        
        node.addMixin("mix:lockable");
        node.getSession().save();
        node.getSession().getWorkspace().getLockManager().lock(node.getPath(), false, false, 1, null);
        
        assertTrue("The node should be locked", node.isLocked());
        Thread.sleep(TimeUnit.SECONDS.toMillis(75));
        assertFalse("The node should not be locked", node.isLocked());
    }
}
