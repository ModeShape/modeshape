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
import java.util.HashMap;
import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.Privilege;

/**
 * Principal based implementation of the Access List.
 * 
 * @author kulikov
 */
public class JcrAccessControlList implements AccessControlList {
   
    //ACL entries
    private HashMap<Principal, AccessControlEntryImpl> principals = new HashMap();
        
    
    @Override
    public AccessControlEntry[] getAccessControlEntries() throws RepositoryException {
        AccessControlEntry[] list = new AccessControlEntry[principals.values().size()];
        principals.values().toArray(list);
        return list;
    }

    @Override
    public boolean addAccessControlEntry(Principal principal, Privilege[] privileges) throws AccessControlException, RepositoryException {
        principals.put(principal, new AccessControlEntryImpl(principal, privileges));
        return true;
    }

    @Override
    public void removeAccessControlEntry(AccessControlEntry accessControlEntry) throws AccessControlException, RepositoryException {
        principals.remove(accessControlEntry.getPrincipal());
    }
    
    /**
     * Tests privileges for the given user.
     * 
     * @param username the name of the user
     * @param privileges privileges for testing
     * @return true if this access list contains all given privileges for 
     * the given user
     */
    public boolean hasPrivileges(String username, Privilege[] privileges) {
        for (AccessControlEntryImpl ace : principals.values()) {
            if (ace.getPrincipal().getName().equals(username)) {
                return ace.hasPrivileges(privileges);
            }
        }
        return false;
    }
    
    /**
     * Lists all privileges defined by this access list for the given user.
     * 
     * @param username the name of the user
     * @return list of privilege objects.
     */
    public Privilege[] getPrivileges(String username) {
        for (AccessControlEntryImpl ace : principals.values()) {
            if (ace.getPrincipal().getName().equals(username)) {
                return ace.getPrivileges();
            }
        }
        return new Privilege[]{};
    }
}
