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

    private JcrAccessControlList acl = new JcrAccessControlList("root");
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
