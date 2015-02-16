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
package org.modeshape.jcr.security;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsNull;
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

        setPolicy("/", Privilege.JCR_ALL);
        setPolicy("/Cars/Luxury/Cadillac DTS", Privilege.JCR_READ, Privilege.JCR_WRITE, Privilege.JCR_MODIFY_ACCESS_CONTROL);
        setPolicy("/Cars/Luxury/", Privilege.JCR_READ, Privilege.JCR_MODIFY_ACCESS_CONTROL, Privilege.JCR_READ_ACCESS_CONTROL);
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
        Matcher<AccessControlManager> m = IsNull.notNullValue(AccessControlManager.class);
        m.matches(session.getAccessControlManager());
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
            //expected
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
            //expected
        }
    } 
    
    @Test
    @FixFor( "MODE-2428" )
    public void shouldCheckPermissionsWhenSettingPropertyValues() throws RepositoryException {
        Node car = session.getNode("/Cars/Luxury/Lexus IS350");
        Property maker = car.getProperty("car:maker");
        Property rating = car.getProperty("car:userRating");
        
        try {
            maker.setValue("some value");
            fail("Should deny modification");
        } catch (AccessDeniedException e) {
            //expected
        }

        try {
            rating.setValue(2);
            fail("Should deny modification");
        } catch (RepositoryException e) {
            //expected
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
            // expected
        }
    }

    @Test
    public void shoudlDenyRemove2() throws RepositoryException {
        Node car = session.getNode("/Cars/Luxury/Cadillac DTS");
        try {
            car.remove();
            fail("Should deny remove operation: Parent node has no privilege to remove child node");
        } catch (AccessDeniedException e) {
            //expected
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
            //expected
        }
    }

    // -------------------- Testing access control api ---
    @Test
    public void onlyAccessControlAPIAllowsRemoveACL() throws Exception {
        Node node = session.getNode("/Cars/Luxury/mode:acl");
        assertThat(node, is(notNullValue()));
        try {
            node.remove();
            fail("Only Access Control API allows modification");
        } catch (AccessDeniedException e) {
            //expected
        }
    }

    @Test
    public void onlyAccessControlAPIAllowsAddACL() throws Exception {
        Node node = session.getNode("/Cars/Hybrid");
        assertThat(node, is(notNullValue()));
        try {
            node.addMixin("mode:accessControllable");
            Node acl = node.addNode("mode:acl", "mode:Acl");
            acl.addNode("test", "mode:Permission");
            fail("Only Access Control API allows modification");
        } catch (ConstraintViolationException e) {
            //expected
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
            //expected
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

        AccessControlList acl2 = acl("/aircraft");
        acl2.addAccessControlEntry(SimplePrincipal.newInstance("Admin"),
                                   new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
        acl2.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                   new Privilege[] {acm.privilegeFromName(Privilege.JCR_READ)});

        acm.setPolicy("/aircraft", acl2);

        AccessControlList acl = acl("/");
        acl.addAccessControlEntry(SimplePrincipal.newInstance("Admin"),
                                  new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
        acl.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                  new Privilege[] {acm.privilegeFromName(Privilege.JCR_READ)});

        acm.setPolicy("/", acl);

        session.save();

        root = session.getRootNode();
        aircraft = root.getNode("aircraft");
    }

    // -------------------------------

    @Test
    public void testGetApplicablePolicies() throws Exception {
        AccessControlList acl = (AccessControlList)acm.getApplicablePolicies("/Cars").nextAccessControlPolicy();
        assertTrue(acl != null);
    }

    @Test
    @FixFor( "MODE-2193" )
    public void shouldAllowReadingAccessibleNodes() throws Exception {
        AccessControlList acl = acl("/");
        acl.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                  new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
        acm.setPolicy("/", acl);

        Node root = session.getRootNode();
        Node ufo = root.addNode("ufo");
        Node vans = root.addNode("vans");
        assertThat(ufo, is(notNullValue()));
        assertThat(vans, is(notNullValue()));

        AccessControlList acl1 = acl("/ufo");
        acl1.addAccessControlEntry(SimplePrincipal.newInstance("Admin"),
                                   new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
        acl1.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"),
                                   new Privilege[] {acm.privilegeFromName(Privilege.JCR_READ)});

        acm.setPolicy("/ufo", acl1);

        //No Access to "anonymous" on "vans" node
        AccessControlList acl2 = acl("/vans");
        acl2.addAccessControlEntry(SimplePrincipal.newInstance("user"),
                                   new Privilege[] {acm.privilegeFromName(Privilege.JCR_ALL)});
        acm.setPolicy("/vans", acl2);

        session.save();

        root = session.getRootNode();
        NodeIterator ni = root.getNodes();

        while(ni.hasNext()){
            ni.nextNode();
        }
    }
    
    @Test
    @FixFor( "MODE-2408" )
    public void shouldVerifyParentACLsIfChildHasEmptyACLList() throws Exception {
        Node parent = ((Node) session.getNode("/")).addNode("parent");
        setPolicy("/parent", Privilege.JCR_ADD_CHILD_NODES, Privilege.JCR_MODIFY_ACCESS_CONTROL, Privilege.JCR_READ_ACCESS_CONTROL);
        session.save();
        
        parent.addNode("child");
        AccessControlList childAcl = acl("/parent/child");
        // set an empty policy on the child node
        acm.setPolicy("/parent/child", childAcl);    
        session.save();

        // modify the parent's ACL to not allow changing of ACLs anymore
        AccessControlList parentAcl = acl("/parent");
        parentAcl.removeAccessControlEntry(parentAcl.getAccessControlEntries()[0]);
        parentAcl.addAccessControlEntry(SimplePrincipal.newInstance("anonymous"), 
                                        new Privilege[]{ acm.privilegeFromName(Privilege.JCR_ADD_CHILD_NODES),
                                                         acm.privilegeFromName(Privilege.JCR_READ_ACCESS_CONTROL)});
        acm.setPolicy("/parent", parentAcl);
        session.save();

        // attempt to modify the child's ACL and verify that it fails because the child has an empty ACL list so we should be really 
        // checking the parent node
        try {
            setPolicy("/parent/child", Privilege.JCR_ALL);
            fail("Should not allow changing ACLs on a node with an empty policy list for which the parent doesn't have the appropriate permissions");
        } catch (AccessDeniedException e) {
            // expected
        }
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


    private boolean contains( String name,
                              Privilege[] privileges ) {
        for (Privilege privilege : privileges) {
            if (name.equals(privilege.getName())) {
                return true;
            }
        }
        return false;
    }
}
