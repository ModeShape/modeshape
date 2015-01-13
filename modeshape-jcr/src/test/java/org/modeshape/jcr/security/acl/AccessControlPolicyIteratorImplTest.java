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
package org.modeshape.jcr.security.acl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.Privilege;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.security.SimplePrincipal;

/**
 * Tests for the AccessControlPolicyIterator implementation.
 * 
 * @author kulikov
 */
public class AccessControlPolicyIteratorImplTest {

    private AccessControlPolicyIteratorImpl it;

    public AccessControlPolicyIteratorImplTest() {
    }

    @Before
    public void setUp() throws AccessControlException, RepositoryException {

        // acl-1
        JcrAccessControlList alice = new JcrAccessControlList("alice");
        alice.addAccessControlEntry(SimplePrincipal.newInstance("alice"), new Privilege[] {new PrivilegeImpl()});

        JcrAccessControlList bob = new JcrAccessControlList("bob");
        bob.addAccessControlEntry(SimplePrincipal.newInstance("bob"), new Privilege[] {new PrivilegeImpl()});

        it = new AccessControlPolicyIteratorImpl(alice, bob);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testNextAccessControlPolicy() throws RepositoryException {
        AccessControlList acl = (AccessControlList)it.nextAccessControlPolicy();
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        assertEquals(1, entries.length);
    }

    @Test
    public void testSkip() {
        assertTrue(it.hasNext());

        it.skip(1);
        assertTrue(it.hasNext());

        it.skip(1);
        assertFalse(it.hasNext());
    }

    @Test
    public void testGetSize() {
        assertEquals(2, it.getSize());
        it.remove();
        assertEquals(1, it.getSize());
    }

    @Test
    public void testGetPosition() {
        assertEquals(0, it.getPosition());
        it.skip(1);

        assertEquals(1, it.getPosition());
    }

    @Test
    public void testHasNext() {
        assertTrue(it.hasNext());

        it.skip(1);
        assertTrue(it.hasNext());

        it.skip(1);
        assertFalse(it.hasNext());
    }

    @Test
    public void testNext() throws RepositoryException {
        AccessControlList acl = (AccessControlList)it.next();
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        assertEquals("alice", entries[0].getPrincipal().getName());

        acl = (AccessControlList)it.next();
        entries = acl.getAccessControlEntries();
        assertEquals("bob", entries[0].getPrincipal().getName());
    }

    @Test
    public void testRemove() {
        assertEquals(2, it.getSize());
        it.remove();
        assertEquals(1, it.getSize());
    }

    protected class PrivilegeImpl implements Privilege {

        @Override
        public String getName() {
            return "jcr:all";
        }

        @Override
        public boolean isAbstract() {
            return false;
        }

        @Override
        public boolean isAggregate() {
            return false;
        }

        @Override
        public Privilege[] getDeclaredAggregatePrivileges() {
            throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools |
                                                                           // Templates.
        }

        @Override
        public Privilege[] getAggregatePrivileges() {
            throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose Tools |
                                                                           // Templates.
        }

    }
}
