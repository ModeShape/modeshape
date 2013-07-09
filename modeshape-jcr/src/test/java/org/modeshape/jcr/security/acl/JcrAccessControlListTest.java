/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.security.acl;

import java.security.Principal;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.modeshape.jcr.security.User;

/**
 *
 * @author kulikov
 */
public class JcrAccessControlListTest {
    
    private JcrAccessControlList acl = new JcrAccessControlList();
    private Privilege[] rw = new Privilege[]{Privileges.READ, Privileges.WRITE};        
    
    public JcrAccessControlListTest() {
    }
    
    @Before
    public void setUp() throws AccessControlException, RepositoryException {
        acl.addAccessControlEntry(User.newInstance("kulikov"), rw);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getAccessControlEntries method, of class JcrAccessControlList.
     */
    @Test
    public void testAccessControlEntries() throws Exception {
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        assertEquals(1, entries.length);
        assertEquals("kulikov", entries[0].getPrincipal().getName());
    }

    /**
     * Test of addAccessControlEntry method, of class JcrAccessControlList.
     */
    @Test
    public void testHasPermission() throws Exception {
        AccessControlEntryImpl entry = (AccessControlEntryImpl) acl.getAccessControlEntries()[0];
        assertTrue(entry.hasPrivileges(rw));
        assertTrue(entry.hasPrivileges(new Privilege[]{Privileges.ADD_CHILD_NODES}));
    }

    
}