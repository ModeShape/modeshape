/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

/**
 *
 * @author kulikov
 */
public class JcrAccessControlListTest {
    
    private JcrAccessControlList acl = new JcrAccessControlList();
    private Privilege[] rw = new Privilege[]{Privileges.READ, Privileges.WRITE};        
    
    public JcrAccessControlListTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws AccessControlException, RepositoryException {
        acl.addAccessControlEntry(new User("kulikov"), rw);
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

    
    private class User implements Principal {
        
        private String username;
        
        public User(String username) {
            this.username = username;
        }
        
        @Override
        public String getName() {
            return username;
        }
        
    }
}