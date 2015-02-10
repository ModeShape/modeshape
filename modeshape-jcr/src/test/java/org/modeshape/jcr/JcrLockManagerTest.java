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

import static org.junit.Assert.fail;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;
import org.junit.Assert;
import org.junit.Test;
import org.modeshape.common.FixFor;

/**
 * Unit test for {@link JcrLockManager}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JcrLockManagerTest extends SingleUseAbstractTest {

    @Test
    @FixFor( "MODE-2047" )
    public void shouldNotAllowLockOnTransientNode() throws Exception {
        AbstractJcrNode testNode = session.getRootNode().addNode("test");
        testNode.addMixin("mix:lockable");
        JcrLockManager lockManager = session.lockManager();
        try {
            lockManager.lock(testNode, true, false, Long.MAX_VALUE, null);
            fail("Transient nodes should not be locked");
        } catch (InvalidItemStateException e) {
            // expected
        }
    }

    @Test
    @FixFor( "MODE-2342" )
    public void lockTokensShouldBeRemovedFromSessionUponLogout() throws Exception {
        final AbstractJcrNode testNode = session.getRootNode().addNode("test");
        final String path = testNode.getPath();
        testNode.addMixin("mix:lockable");
        session.save();
        final Lock lock = session.getWorkspace().getLockManager().lock(path,
                false, false, Long.MAX_VALUE, session.getUserID());
        final String token = lock.getLockToken();
        Assert.assertNotNull(token);
        session.logout();

        Session session2 = repository.login();
        final LockManager lockManager = session2.getWorkspace().getLockManager();
        lockManager.addLockToken(token);
        Assert.assertTrue("New session should now own the lock.", lockManager.getLock(path).isLockOwningSession());
    }
    
    @Test
    @FixFor( "MODE-2424" )
    public void shouldAllowAddingMixinOnLockedNodeForLockOwner() throws  Exception {
        final AbstractJcrNode testNode = session.getRootNode().addNode("test");
        final String path = testNode.getPath();
        testNode.addMixin("mix:lockable");
        session.save();
        session.getWorkspace().getLockManager().lock(path, false, true, Long.MAX_VALUE, session.getUserID());
        
        testNode.addMixin("mix:created");        
        session.save();
    }
}
