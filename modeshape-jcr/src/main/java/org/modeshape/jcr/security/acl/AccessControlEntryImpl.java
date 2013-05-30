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
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.Privilege;

/**
 * Implementation for the Access Control entry record.
 * 
 * @author kulikov
 */
public class AccessControlEntryImpl implements AccessControlEntry {

    private Principal principal;
    private Privilege[] privileges;
    
    public AccessControlEntryImpl(Principal principal, Privilege[] privileges) {
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
}
