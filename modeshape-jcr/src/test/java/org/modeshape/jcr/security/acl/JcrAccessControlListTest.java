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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.security.SimplePrincipal;

/**
 * @author kulikov
 */
public class JcrAccessControlListTest extends MultiUseAbstractTest {

    private JcrAccessControlList acl = new JcrAccessControlList(null, "root");
    private Privilege[] rw;
    private Privileges privileges;

    public JcrAccessControlListTest() {
    }

    @BeforeClass
    public static final void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

        // Import the node types and the data ...
        registerNodeTypes("cars.cnd");
        importContent("/", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

    }

    @AfterClass
    public static final void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Before
    public void setUp() throws AccessControlException, RepositoryException {
        privileges = new Privileges(session);
        rw = new Privilege[] {privileges.forName(Privilege.JCR_READ), privileges.forName(Privilege.JCR_WRITE)};
        acl.addAccessControlEntry(SimplePrincipal.newInstance("kulikov"), rw);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAccessControlEntries() throws Exception {
        AccessControlEntry[] entries = acl.getAccessControlEntries();
        assertEquals(1, entries.length);
        assertEquals("kulikov", entries[0].getPrincipal().getName());
    }

    @Test
    public void testHasPermission() throws Exception {
        AccessControlEntryImpl entry = (AccessControlEntryImpl)acl.getAccessControlEntries()[0];
        assertTrue(entry.hasPrivileges(rw));
        assertTrue(entry.hasPrivileges(new Privilege[] {privileges.forName(Privilege.JCR_ADD_CHILD_NODES)}));
    }

}
