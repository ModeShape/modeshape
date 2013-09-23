/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.shared;

import com.smartgwt.client.widgets.grid.ListGridRecord;
import java.util.Collection;
import javax.jcr.security.Privilege;

/**
 *
 * @author kulikov
 */
public class JcrPermissions {
    public static JcrPermission[] PERMISSIONS = new JcrPermission[]{
        new JcrPermission(Privilege.JCR_ADD_CHILD_NODES, "Add child nodes"),
        new JcrPermission(Privilege.JCR_ALL, "All permissions"),
        new JcrPermission(Privilege.JCR_LIFECYCLE_MANAGEMENT, "Life cycle management"),
        new JcrPermission(Privilege.JCR_LOCK_MANAGEMENT, "Lock management"),
        new JcrPermission(Privilege.JCR_MODIFY_ACCESS_CONTROL, "Modify access control"),
        new JcrPermission(Privilege.JCR_MODIFY_PROPERTIES, "Modify properties"),
        new JcrPermission(Privilege.JCR_NODE_TYPE_MANAGEMENT, "Node type management"),
        new JcrPermission(Privilege.JCR_READ, "Read"),
        new JcrPermission(Privilege.JCR_READ_ACCESS_CONTROL, "Read access control"),
        new JcrPermission(Privilege.JCR_REMOVE_CHILD_NODES, "Remove child nodes"),
        new JcrPermission(Privilege.JCR_REMOVE_NODE, "Remove node"),
        new JcrPermission(Privilege.JCR_RETENTION_MANAGEMENT, "Retention management"),
        new JcrPermission(Privilege.JCR_VERSION_MANAGEMENT, "Version management"),
        new JcrPermission(Privilege.JCR_WRITE, "Write"),

    };
    
    public ListGridRecord[] test(Collection<JcrPermission> permissions) {
        ListGridRecord[] records = new ListGridRecord[PERMISSIONS.length];
        for (int i = 0; i < records.length; i++) {
            records[i] = new ListGridRecord();
            records[i].setAttribute("permission", PERMISSIONS[i].getDisplayName());
            records[i].setAttribute("status", status(permissions, PERMISSIONS[i]));
        }
        return records;
    }
    
    private String status(Collection<JcrPermission>  list, JcrPermission value) {
        for (JcrPermission p : list) {
            if (p.getName().equals(value.getName())) {
                return "Allow";
            }
        }
        
        return "Deny";
    }
}
