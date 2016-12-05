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

import com.smartgwt.client.widgets.grid.ListGridRecord;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Access control list.
 * 
 * @author kulikov
 */
public class JcrAccessControlList implements Serializable {
    private static JcrPermission[] PERMISSIONS = new JcrPermission[]{
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
        JcrPermission.REMOVE_CHILD_NODES
    };
    
    private ArrayList<JcrACLEntry> entries = new ArrayList();
    
    public JcrAccessControlList() {
    }
    
    public void add(JcrACLEntry entry) {
        entries.add(entry);
    }
    
    public void remove(JcrACLEntry entry) {
        entries.remove(entry);
    }
    
    public Collection<JcrACLEntry> entries() {
        return entries;
    }
    
    /**
     * Searches entry for the given principal.
     * 
     * @param principal the name of the principal.
     * @return  entry for the specified principal.
     */
    public JcrACLEntry find(String principal) {
        for (JcrACLEntry entry : entries) {
            if (entry.getPrincipal().equals(principal)) {
                return entry;
            }
        }
        return null;
    }
    
    public ListGridRecord[] test(String principal) {
        JcrACLEntry entry = find(principal);
        
        ListGridRecord[] records = new ListGridRecord[PERMISSIONS.length];
        for (int i = 0; i < records.length; i++) {
            records[i] = new ListGridRecord();
            records[i].setAttribute("icon", "blue");
            records[i].setAttribute("permission", PERMISSIONS[i].getDisplayName());
            records[i].setAttribute("status", status(entry.getPermissions(), PERMISSIONS[i]));
        }
        return records;
    }
    
    private String status(Collection<JcrPermission>  list, JcrPermission value) {
        for (JcrPermission p : list) {
            if (p.matches(value)) {
                return "Allow";
            } 
        }
        
        return "Deny";
    }
    
}
