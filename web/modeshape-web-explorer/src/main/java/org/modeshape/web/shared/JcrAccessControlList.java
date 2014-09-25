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
package org.modeshape.web.shared;

import com.smartgwt.client.util.SC;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * Access control list.
 * 
 * @author kulikov
 */
public class JcrAccessControlList implements Serializable {
    private static final long serialVersionUID = 1L;

    private static JcrPermission[] PERMISSIONS = new JcrPermission[] {
        JcrPermission.ALL, 
        JcrPermission.LIFECYCLE_MANAGEMENT,
        JcrPermission.LOCK_MANAGEMENT, 
        JcrPermission.NODE_TYPE_MANAGEMENT, 
        JcrPermission.RETENTION_MANAGEMENT,
        JcrPermission.VERSION_MANAGEMENT, 
        JcrPermission.READ_ACCESS_CONTROL, 
        JcrPermission.MODIFY_ACCESS_CONTROL,
        JcrPermission.READ, 
        JcrPermission.WRITE, 
        JcrPermission.ADD_CHILD_NODES, 
        JcrPermission.MODIFY_PROPERTIES,
        JcrPermission.REMOVE_CHILD_NODES};

    private ArrayList<JcrPolicy> entries = new ArrayList<JcrPolicy>();
    private boolean isModified = false;

    public static JcrAccessControlList defaultInstance() {
        return new JcrAccessControlList(JcrPolicy.everyone());
    }

    public JcrAccessControlList() {
    }

    /**
     * Creates access control list.
     * 
     * @param policy list of policies.
     */
    public JcrAccessControlList( JcrPolicy... policy ) {
        entries.addAll(Arrays.asList(policy));
    }

    /**
     * Includes policy into this access list.
     * 
     * @param entry policy to add
     */
    public void add( JcrPolicy entry ) {
        entries.add(entry);
        this.isModified = true;
    }

    /**
     * Excludes policy from this list.
     * 
     * @param entry the policy to exclude.
     */
    public void remove( JcrPolicy entry ) {
        entries.remove(entry);
        this.isModified = true;
    }

    /**
     * Removes policy for the given principal.
     * 
     * @param principal
     */
    public void remove( String principal ) {
        JcrPolicy policy = find(principal);
        if (policy != null) {
            entries.remove(policy);
            this.isModified = true;
        } else {
            SC.say("principal not found");
        }
    }

    public Collection<JcrPolicy> entries() {
        return entries;
    }

    /**
     * Searches policy for the given principal.
     * 
     * @param principal the name of the principal.
     * @return entry for the specified principal.
     */
    public JcrPolicy find( String principal ) {
        for (JcrPolicy entry : entries) {
            if (entry.getPrincipal().equals(principal)) {
                return entry;
            }
        }
        return null;
    }

    public ListGridRecord[] test( String principal ) {
        JcrPolicy entry = find(principal);

        ListGridRecord[] records = new ListGridRecord[PERMISSIONS.length];
        for (int i = 0; i < records.length; i++) {
            String status = status(entry.getPermissions(), PERMISSIONS[i]);
            records[i] = new ListGridRecord();
            records[i].setAttribute("icon", "blue");
            records[i].setAttribute("permission", PERMISSIONS[i].getDisplayName());
            records[i].setAttribute("sign", status.toLowerCase());
            records[i].setAttribute("status", status);
        }
        return records;
    }

    private String status( Collection<JcrPermission> list,
                           JcrPermission value ) {
        for (JcrPermission p : list) {
            if (p.matches(value)) {
                return "Allow";
            }
        }

        return "Deny";
    }

    /**
     * Updates this list.
     * 
     * @param principal the principal name
     * @param action action to modify
     * @param value the new value for the permission.
     */
    public void modify( String principal,
                        String action,
                        String value ) {
        JcrPolicy entry = find(principal);
        entry.update(action, value);
        this.isModified = true;
    }

    public boolean isModified() {
        return this.isModified;
    }

    public void cleanModificationFlag() {
        this.isModified = false;
    }
    
    public String[] principals() {
        String[] res = new String[entries.size()];
        int i = 0;
        for (JcrPolicy p : entries) {
            res[i++] = p.getPrincipal();
        }
        return res;
    }
}
