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
import java.util.List;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;

/**
 * Implementation for the Access Control entry record.
 * 
 * An AccessControlEntry represents the association of one or more 
 * <code>Privilege</code> objects with a specific <code>Principal</code>. 
 * 
 * @author kulikov
 */
public class AccessControlEntryImpl implements AccessControlEntry {

    private Principal principal;
    private Privilege[] privileges;
    
    /**
     * Creates new ACL entry.
     * 
     * @param principal principal associated with this entry.
     * @param privileges one or more privilege in association with given principal.
     */
    public AccessControlEntryImpl(Principal principal, Privilege[] privileges) throws AccessControlException {
        for (Privilege p : privileges) {
            if (p.getName() == null) 
                throw new AccessControlException("Invalid privilege");
        }
        this.principal = principal;
        this.privileges = privileges;
    }
    
    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Privilege[] getPrivileges() {
        return privileges;
    }
    
    /**
     * Tests given privileges.
     * 
     * @param privileges privileges for testing.
     * @return true this entry contains all given privileges
     */
    protected boolean hasPrivileges(Privilege[] privileges) {
        for (Privilege p : privileges) {
            if (!contains(this.privileges, p)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Tests given privilege.
     * 
     * @param privileges set of privileges.
     * @param p given privilege for testing
     * @return true privilege set contains given privilege.
     */
    private boolean contains(Privilege[] privileges, Privilege p) {
        for (int i = 0; i < privileges.length; i++) {
            if (((PrivilegeImpl)privileges[i]).contains(p)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(List<Privilege>privileges, Privilege p) {
        for (int i = 0; i < privileges.size(); i++) {
            if (((PrivilegeImpl)privileges.get(i)).contains(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds specified privileges to this entry.
     * 
     * @param privileges privileges to add.
     * @return true if at least one of privileges was added.
     */
    protected boolean addIfNotPresent(Privilege[] privileges) {
        ArrayList<Privilege> list = new ArrayList();
        list.addAll(Arrays.asList(this.privileges));
        
        boolean res = combineRecursively(list, privileges);

        this.privileges = new Privilege[list.size()];
        list.toArray(this.privileges);
        
        return res;
    }

    /**
     * Adds specified privileges to this entry.
     * 
     * @param privileges privileges to add.
     * @return true if at least one of privileges was added.
     */
    protected boolean combineRecursively(List<Privilege>list, Privilege[] privileges) {
        boolean res = false;
        
        for (Privilege p : privileges) {
            if (p.isAggregate()) {
                res = combineRecursively(list, p.getAggregatePrivileges());
            } else if (!contains(list, p)) {
                list.add(p);
                res = true;
            }
        }
        return res;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        
        if (!(other instanceof AccessControlEntryImpl)) {
            return false;
        }
        
        AccessControlEntryImpl entry = (AccessControlEntryImpl) other;
        
        if (!entry.principal.equals(principal)) {
            return false;
        }
        
        if (this.privileges.length != entry.privileges.length) {
            return false;
        }
        
        for (int i = 0; i < privileges.length; i++) {
            if (!contains(entry.privileges, privileges[i])) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + (this.principal != null ? this.principal.hashCode() : 0);
        return hash;
    }
}
