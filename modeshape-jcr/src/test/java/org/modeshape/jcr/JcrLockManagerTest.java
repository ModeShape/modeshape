package org.modeshape.jcr;

import static org.junit.Assert.fail;
import javax.jcr.InvalidItemStateException;
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
            //expected
        }
    }
}
