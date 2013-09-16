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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.Privilege;
import org.modeshape.jcr.AccessControlManagerImpl;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.security.SimplePrincipal;

/**
 * Resources based Access Control List implementation.
 * 
 * ACLs are stored per node in a special child node called 
 * <bold>mode:acl</bold>. This node has a list of <bold> mode:{$principal_name}</bold> 
 * child  nodes which has multi-value property permissions. 
 * Permissions are defined by JCR specifications.
 * 
 * {node} {mode:AccessControllable}
 *	+mode:acl{mode:Acl}
 *		+user-name{mode:permission}
 *			-permissions {String}
 * 
 * 
 * @author kulikov
 */
public class JcrAccessControlList implements AccessControlList {
   
    //ACL entries
    private HashMap<Principal, AccessControlEntryImpl> principals = 
            new HashMap<Principal, AccessControlEntryImpl>();
    
    //path to the node to which this ACL belongs
    private final String path;    
    
    /**
     * Creates default Access Control List.
     * 
     * @param acm access control manager instance
     * @return Access Control List with all permissions granted to everyone.
     */
    public static JcrAccessControlList defaultAcl(AccessControlManagerImpl acm) {
        JcrAccessControlList acl = new JcrAccessControlList(acm, "/");
        try {
            acl.principals.put(SimplePrincipal.EVERYONE, 
                new AccessControlEntryImpl(SimplePrincipal.EVERYONE, acm.privileges()));
        } catch (AccessControlException e) {
            //will never happen
        }
        return acl;
    }
    
    /**
     * Creates new empty access control list.
     * 
     * @param acm Access Control Manager managing this access list.
     * @param path the path to which this access list is applied.
     */
    public JcrAccessControlList(AccessControlManagerImpl acm, String path) {
        this.path = path;
    }
    
    /**
     * Checks entries of this access control list.
     * 
     * @return true if this access list does not have entries and false otherwise.
     */
    public boolean isEmpty() {
        return principals.isEmpty();
    }
    
    @Override
    public AccessControlEntry[] getAccessControlEntries() {
        AccessControlEntry[] list = new AccessControlEntry[principals.values().size()];
        principals.values().toArray(list);
        return list;
    }

    @Override
    public boolean addAccessControlEntry(Principal principal, Privilege[] privileges) throws AccessControlException, RepositoryException {
        if (privileges == null || privileges.length == 0) {
            throw new AccessControlException("Invalid privilege array");
        }
        
        if (principal.getName().equals("unknown")) {
            throw new AccessControlException("Unknown principal");
        }
        //Jast new entry
        if (!principals.containsKey(principal)) {
            principals.put(principal, new AccessControlEntryImpl(principal, privileges));
            return true;
        }
        
        //there is entry for the given principal so jast add missing privileges
        AccessControlEntryImpl ace = principals.get(principal);
        return ace.addIfNotPresent(privileges);
    }

    @Override
    public void removeAccessControlEntry(AccessControlEntry accessControlEntry) throws AccessControlException, RepositoryException {        
        AccessControlEntry entry = principals.remove(accessControlEntry.getPrincipal());
        if (entry == null) throw new AccessControlException("Invalid access control entry");
    }
    
    /**
     * Tests privileges relatively to the given security context.
     * 
     * @param sc security context carrying information about principals
     * @param privileges privileges for test
     * @return true when access list grants all given privileges within given security context.
     */
    public boolean hasPrivileges(SecurityContext sc, Privilege[] privileges) {
        for (AccessControlEntryImpl ace : principals.values()) {
            //check access list for everyone
            if (ace.getPrincipal().getName().equals(SimplePrincipal.EVERYONE.getName())) {
                if (ace.hasPrivileges(privileges)) {
                    return true;
                }
            }            
            
            //check user principal
            if (ace.getPrincipal().getName().equals(username(sc.getUserName()))) {
                if (ace.hasPrivileges(privileges)) {
                    return true;
                }
            }
            
            //check group/role principal
            if (sc.hasRole(ace.getPrincipal().getName())) {
                if (ace.hasPrivileges(privileges)) {
                    return true;
                }
            }
        } 
        return false;
    }

    /**
     * Lists all privileges defined by this access list for the given user.
     * 
     * @param context the security context of the user; never null
     * @return list of privilege objects.
     */
    public Privilege[] getPrivileges(SecurityContext context) {
        ArrayList<Privilege> privs = new ArrayList<Privilege>();
        for (AccessControlEntryImpl ace : principals.values()) {
            //add privileges granted for everyone
            if (ace.getPrincipal().equals(SimplePrincipal.EVERYONE)) {
                privs.addAll(Arrays.asList(ace.getPrivileges()));
            }
            
            //add privileges granted for given user
            if (ace.getPrincipal().getName().equals(username(context.getUserName()))) {
                privs.addAll(Arrays.asList(ace.getPrivileges()));
            }
            
            //add privileges granted for given role
            if (context.hasRole(ace.getPrincipal().getName())) {
                privs.addAll(Arrays.asList(ace.getPrivileges()));
            }
        }
        
        Privilege[] res = new Privilege[privs.size()];
        privs.toArray(res);
        
        return res;
    }
        
    public boolean hasEntry(String name) {
        AccessControlEntry[] entries = this.getAccessControlEntries();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].getPrincipal().getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        
        if (!(other instanceof JcrAccessControlList)) {
            return false;
        }
        
        return ((JcrAccessControlList)other).path.equals(path);
    }

    @Override
    public int hashCode() {
        return this.path.hashCode();
    }
    
    /**
     * Removes brackets enclosing given user name
     * 
     * @param username the user name 
     * @return user name without brackets.
     */
    private String username(String username) {
        return (username.startsWith("<") && username.endsWith(">")) ?
                username.substring(1, username.length() -1) : username;
    }
}
