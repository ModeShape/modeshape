/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.shared;

import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import java.util.Collection;

/**
 *
 * @author kulikov
 */
public class JcrPermissions {
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
    
    public ListGridRecord[] test(Collection<JcrPermission> permissions) {
        ListGridRecord[] records = new ListGridRecord[PERMISSIONS.length];
        for (int i = 0; i < records.length; i++) {
            records[i] = new ListGridRecord();
            records[i].setAttribute("icon", "blue");
            records[i].setAttribute("permission", PERMISSIONS[i].getDisplayName());
            records[i].setAttribute("status", status(permissions, PERMISSIONS[i]));
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
