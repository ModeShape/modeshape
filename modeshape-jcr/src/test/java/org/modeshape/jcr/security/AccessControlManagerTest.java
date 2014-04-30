/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.jcr.security;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.security.AccessControlException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.security.acl.Privileges;

public class AccessControlManagerTest extends MultiUseAbstractTest {

    private AccessControlManager acm;
    private Privileges privileges;

    @BeforeClass
    public static final void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

        // Import the node types and the data ...
        registerNodeTypes("cars.cnd");
        importContent("/", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);

        setPolicy("/Cars/Luxury/Cadillac DTS", Privilege.JCR_READ, Privilege.JCR_WRITE, Privilege.JCR_MODIFY_ACCESS_CONTROL);
        setPolicy("/Cars/Luxury/", Privilege.JCR_READ, Privilege.JCR_MODIFY_ACCESS_CONTROL);
        setPolicy("/Cars/Sports/", Privilege.JCR_READ, Privilege.JCR_WRITE, Privilege.JCR_MODIFY_ACCESS_CONTROL);
        setPolicy("/Cars/Utility/Ford F-150/", Privilege.JCR_MODIFY_ACCESS_CONTROL, Privilege.JCR_READ_ACCESS_CONTROL);
        setPolicy("/Cars/Utility/", Privilege.JCR_READ_ACCESS_CONTROL);

    }

    @AfterClass
    public static final void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        acm = session.getAccessControlManager();
        privileges = new Privileges(session);
    }

    @Test
    public void testSecondSession() throws Exception {
        Session session2 = session.getRepository().login();
        session2.logout();
    }

    @Test
    public void shouldObtainAccessControlManager() throws Exception {
        assertTrue(acm != null);
        assertNotNull(session.getAccessControlManager());
    }

    @Test
    public void testGetSupportedPrivileges() throws Exception {
        Privilege[] permissions = acm.getSupportedPrivileges("/");
        assertEquals(privileges.listOfSupported().length, permissions.length);
    }

    @Test
    public void testPrivilegeForName() throws Exception {
        Privilege p = acm.privilegeFromName(Privilege.JCR_ALL);
        assertEquals("jcr:all", p.getName());
    }

    // --------------- Testing access list -------------------------------------/
    @Test
    public void shouldHaveReadPrivilege() throws Exception {
        Privilege[] privileges = acm.getPrivileges("/Cars/Luxury");
        assertEquals("jcr:read", privileges[0].getName());
    }

    @Test
    public void shoudlHaveReadWritePrivilege() throws Exception {
        Privilege[] privileges = acm.getPrivileges("/Cars/Luxury/Cadillac DTS");
        assertTrue(contains("jcr:read", privileges));
        assertTrue(contains("jcr:write", privileges));
    }

    @Test
    public void shoudlDeriveAccessList() throws Exception {
        Privilege[] privileges = acm.getPrivileges("/Cars/Luxury/Lexus IS350");
        assertEquals("jcr:read", privileges[0].getName());
    }

    @Test
    public void shoudlGrantAllPermissions() throws Exception {
        Privilege[] privileges = acm.getPrivileges("/Cars/Hybrid");
        assertTrue(contains("jcr:all", privileges));
    }

    @Test
    public void shouldGrantAdd() throws Exception {
        Node sports = session.getNode("/Cars/Sports");
        try {
            sports.addNode("Chevrolet Camaro", "car:Car");
        } catch (AccessDeniedException e) {
            fail("Should grant add");
        }
    }

    @Test
    public void shouldDenyAdd() throws RepositoryException {
        Node luxury = session.getNode("/Cars/Luxury");
        try {
            luxury.addNode("Cadillac Flitwood", "car:Car");
            fail("Should deny add node");
        } catch (AccessDeniedException e) {
            System.out.println("Hide exception");
        } catch (AccessControlException e) {
            System.out.println("Hide exception");
        }
    }

    @Test
    public void shouldGrantModify() throws RepositoryException {
        Node infinity = session.getNode("/Cars/Sports/Infiniti G37");
        try {
            infinity.setProperty("car:msrp", "$34,901");
        } catch (AccessDeniedException e) {
            fail("Should grant modification");
        }
    }

    @Test
    public void shouldDenyModify() throws RepositoryException {
        Node car = session.getNode("/Cars/Luxury/Lexus IS350");
        try {
            car.setProperty("car:msrp", "$34,901");
            fail("Should deny modification");
        } catch (AccessDeniedException e) {
        }
    }

    @Test
    public void shouldGrantRemove() throws RepositoryException {
        Node car = session.getNode("/Cars/Sports/Infiniti G37");
        try {
            car.remove();
        } catch (AccessDeniedException e) {
            fail("Should grant remove operation");
        }
    }

    @Test
    public void shouldDenyRemove() throws RepositoryException {
        Node car = session.getNode("/Cars/Luxury/Lexus IS350");
        try {
            car.remove();
            fail("Should deny remove operation");
        } catch (AccessDeniedException e) {
        } catch (AccessControlException e) {
        }
    }

    @Test
    public void shoudlDenyRemove2() throws RepositoryException {
        Node car = session.getNode("/Cars/Luxury/Cadillac DTS");
        try {
            car.remove();
            fail("Should deny remove operation: Parent node has no privilege to remove child node");
        } catch (AccessDeniedException e) {
        } catch (AccessControlException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void shouldAllowSetPolicy() throws RepositoryException {
        setPolicy("/Cars/Utility/Ford F-150", Privilege.JCR_ALL);
    }

    @Test
    public void shouldDenySetPolicy() throws RepositoryException {
        try {
            setPolicy("/Cars/Utility", Privilege.JCR_ALL);
            fail("Should deny access list modification");
        } catch (AccessDeniedException e) {
        }
    }

    // @Test
    public void shouldRemovePolicy() throws RepositoryException {
        acm.removePolicy("/Cars/Utility/Ford F-150", null);
        Privilege[] privileges = acm.getPrivileges("/Cars/Utility/Ford F-150");
        assertEquals(Privilege.JCR_ALL, privileges[0].getName());
    }

    // -------------------- Testing access control api ---
    @Test
    public void onlyAccessControlAPIAllowsRemoveACL() throws Exception {
        Node node = session.getNode("/Cars/Luxury/mode:acl");
        assertThat(node, is(notNullValue()));
        try {
            node.remove();
            fail("Only Access Control API allows modification");
        } catch (Exception e) {
        }
    }

    @Test
    public void onlyAccessControlAPIAllowsAddACL() throws Exception {
        Node node = session.getNode("/Cars/Hybrid");
        assertThat(node, is(notNullValue()));
        try {
            node.addMixin("mix:accessControllable");
            Node acl = node.addNode("mode:acl", "mode:Acl");
            acl.addNode("test", "mode:Permission");
            fail("Only Access Control API allows modification");
        } catch (RepositoryException e) {
        }
    }

    @Test
    public void shouldNotDependFromContentPermissions() throws Exception {
        setPolicy("/Cars/Luxury/Bentley Continental", Privilege.JCR_WRITE);
    }

    @Test
    @FixFor( "MODE-2036" )
    public void shouldDenyAccessChildNode() throws Exception {
        Node root = session.getRootNode();
        Node truks = root.addNode("truks");
        session.save();

        AccessControlManager acm = session.getAccessControlManager();
        Privilege[] privileges = new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)};

        AccessControlList acl;
        AccessControlPolicyIterator it = acm.getApplicablePolicies(truks.getPath());
        if (it.hasNext()) {
            acl = (AccessControlList)it.nextAccessControlPolicy();
        } else {
            acl = (AccessControlList)acm.getPolicies(truks.getPath())[0];
        }
        acl.addAccessControlEntry(SimplePrincipal.newInstance("Admin"), privileges);

        acm.setPolicy(truks.getPath(), acl);
        session.save();

        try {
            root.getNode("truks");
            fail("Access list should deny access");
        } catch (javax.jcr.security.AccessControlException e) {
        }
    }

    @Test
    public void shouldAllowAccessUsingRole() throws Exception {
        Node root = session.getRootNode();
        Node truks = root.addNode("tractors");
        session.save();

        AccessControlManager acm = session.getAccessControlManager();
        Privilege[] privileges = new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)};

        AccessControlList acl;
        AccessControlPolicyIterator it = acm.getApplicablePolicies(truks.getPath());
        if (it.hasNext()) {
            acl = (AccessControlList)it.nextAccessControlPolicy();
        } else {
            acl = (AccessControlList)acm.getPolicies(truks.getPath())[0];
        }
        acl.addAccessControlEntry(SimplePrincipal.newInstance("admin"), privileges);

        acm.setPolicy(truks.getPath(), acl);
        session.save();

        Node node = root.getNode("tractors");
        assertThat(node, is(notNullValue()));
    }

    @Test
    public void shouldAllowRead() throws Exception {
        Node root = session.getRootNode();
        Node aircraft = root.addNode("aircraft");
        assertThat(aircraft, is(notNullValue()));

        AccessControlList acl2 = getACL("/aircraft");
        acl2.addAccessControlEntry(SimplePrincipal.newInstance("Admin"),
                                   new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
        acl2.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                   new Privilege[] {acm.privilegeFromName(Privilege.JCR_READ)});

        acm.setPolicy("/aircraft", acl2);

        root = session.getRootNode();
        aircraft = root.getNode("aircraft");
    }

    // -------------------------------

    @Test
    public void testGetApplicablePolicies() throws Exception {
        AccessControlList acl = (AccessControlList)acm.getApplicablePolicies("/Cars").nextAccessControlPolicy();
        assertTrue(acl != null);
    }

    private static void setPolicy( String path,
                                   String... privileges ) throws UnsupportedRepositoryOperationException, RepositoryException {
        AccessControlManager acm = session.getAccessControlManager();

        Privilege[] permissions = new Privilege[privileges.length];
        for (int i = 0; i < privileges.length; i++) {
            permissions[i] = acm.privilegeFromName(privileges[i]);
        }

        AccessControlList acl = null;
        AccessControlPolicyIterator it = acm.getApplicablePolicies(path);
        if (it.hasNext()) {
            acl = (AccessControlList)it.nextAccessControlPolicy();
        } else {
            acl = (AccessControlList)acm.getPolicies(path)[0];
        }
        acl.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"), permissions);

        acm.setPolicy(path, acl);
        session.save();
    }

    private AccessControlList getACL( String path ) throws Exception {
        AccessControlPolicyIterator it = acm.getApplicablePolicies(path);
        if (it.hasNext()) {
            return (AccessControlList)it.nextAccessControlPolicy();
        }
        return (AccessControlList)acm.getPolicies(path)[0];
    }

    private boolean contains( String name,
                              Privilege[] privileges ) {
        for (int i = 0; i < privileges.length; i++) {
            if (name.equals(privileges[i].getName())) {
                return true;
            }
        }
        return false;
    }

}
