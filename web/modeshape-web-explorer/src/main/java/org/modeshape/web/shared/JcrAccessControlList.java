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
package org.modeshape.web.shared;

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

    private static JcrPermission[] PERMISSIONS = new JcrPermission[] {JcrPermission.ALL, JcrPermission.LIFECYCLE_MANAGEMENT,
        JcrPermission.LOCK_MANAGEMENT, JcrPermission.NODE_TYPE_MANAGEMENT, JcrPermission.RETENTION_MANAGEMENT,
        JcrPermission.VERSION_MANAGEMENT, JcrPermission.READ_ACCESS_CONTROL, JcrPermission.MODIFY_ACCESS_CONTROL,
        JcrPermission.READ, JcrPermission.WRITE, JcrPermission.ADD_CHILD_NODES, JcrPermission.MODIFY_PROPERTIES,
        JcrPermission.REMOVE_CHILD_NODES};

    private ArrayList<JcrPolicy> entries = new ArrayList();
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
}
