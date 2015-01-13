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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.Privilege;
import org.modeshape.common.util.HashCode;

/**
 * Implementation for the Access Control entry record. An AccessControlEntry represents the association of one or more
 * <code>Privilege</code> objects with a specific <code>Principal</code>.
 * 
 * @author kulikov
 */
public class AccessControlEntryImpl implements AccessControlEntry {

    private Principal principal;
    private ArrayList<Privilege> privileges = new ArrayList<Privilege>();

    /**
     * Creates new ACL entry.
     * 
     * @param principal principal associated with this entry.
     * @param privileges one or more privilege in association with given principal.
     * @throws AccessControlException if one or more privileges are invalid.
     */
    public AccessControlEntryImpl( Principal principal,
                                   Privilege[] privileges ) throws AccessControlException {
        for (Privilege p : privileges) {
            if (p.getName() == null) throw new AccessControlException("Invalid privilege");
        }
        assert principal != null;
        this.principal = principal;
        this.privileges.clear();
        Collections.addAll(this.privileges, privileges);
    }

    @Override
    public Principal getPrincipal() {
        return principal;
    }

    @Override
    public Privilege[] getPrivileges() {
        return privileges.toArray(new Privilege[privileges.size()]);
    }

    /**
     * Tests given privileges.
     * 
     * @param privileges privileges for testing.
     * @return true if this entry contains all given privileges
     */
    protected boolean hasPrivileges( Privilege[] privileges ) {
        for (Privilege p : privileges) {
            if (!contains(this.privileges, p)) {
                return false;
            }
        }
        return true;
    }

    private boolean contains( List<Privilege> privileges,
                              Privilege p ) {
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
    protected boolean addIfNotPresent( Privilege[] privileges ) {
        ArrayList<Privilege> list = new ArrayList<Privilege>();
        Collections.addAll(list, privileges);

        boolean res = combineRecursively(list, privileges);

        this.privileges.addAll(list);
        return res;
    }

    /**
     * Adds specified privileges to the given list.
     * 
     * @param list the result list of combined privileges.
     * @param privileges privileges to add.
     * @return true if at least one of privileges was added.
     */
    protected boolean combineRecursively( List<Privilege> list,
                                          Privilege[] privileges ) {
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
    public boolean equals( Object other ) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof AccessControlEntryImpl)) {
            return false;
        }

        AccessControlEntryImpl entry = (AccessControlEntryImpl)other;

        if (!entry.principal.equals(principal)) {
            return false;
        }

        if (this.privileges.size() != entry.privileges.size()) {
            return false;
        }

        for (int i = 0; i < privileges.size(); i++) {
            if (!contains(entry.privileges, privileges.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return HashCode.compute(principal, privileges);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ACE: ");
        sb.append(principal.getName());
        sb.append("=").append(privileges);
        return sb.toString();
    }
}
