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
import java.util.Collection;
import javax.jcr.security.Privilege;

/**
 *
 * @author kulikov
 */
public class Policy implements Serializable {
    
    private JcrPermission LIFECYCLE_MANAGEMENT = new JcrPermission(Privilege.JCR_LIFECYCLE_MANAGEMENT, "Life cycle management");
    private JcrPermission LOCK_MANAGEMENT = new JcrPermission(Privilege.JCR_LOCK_MANAGEMENT, "Lock management");
    private JcrPermission NODE_TYPE_MANAGEMENT = new JcrPermission(Privilege.JCR_NODE_TYPE_MANAGEMENT, "Node type management");
    private JcrPermission RETENTION_MANAGEMENT = new JcrPermission(Privilege.JCR_RETENTION_MANAGEMENT, "Retention management");
    private JcrPermission VERSION_MANAGEMENT = new JcrPermission(Privilege.JCR_VERSION_MANAGEMENT, "Version management");
    
    private JcrPermission READ_ACCESS_CONTROL = new JcrPermission(Privilege.JCR_READ_ACCESS_CONTROL, "Read access control");
    private JcrPermission MODIFY_ACCESS_CONTROL = new JcrPermission(Privilege.JCR_MODIFY_ACCESS_CONTROL, "Modify access control");

    private JcrPermission READ = new JcrPermission(Privilege.JCR_READ, "Read");
    
    private JcrPermission ADD_CHILD_NODES = new JcrPermission(Privilege.JCR_ADD_CHILD_NODES, "Add child nodes");
    private JcrPermission REMOVE_CHILD_NODES = new JcrPermission(Privilege.JCR_REMOVE_CHILD_NODES, "Remove child nodes");
    private JcrPermission MODIFY_PROPERTIES = new JcrPermission(Privilege.JCR_MODIFY_PROPERTIES, "Modify properties");
    private JcrPermission WRITE = new JcrPermission(Privilege.JCR_WRITE, "Write",
            ADD_CHILD_NODES, REMOVE_CHILD_NODES, MODIFY_PROPERTIES);

    private JcrPermission ALL = new JcrPermission(Privilege.JCR_ALL, "All permissions",
            LIFECYCLE_MANAGEMENT, 
            LOCK_MANAGEMENT, 
            NODE_TYPE_MANAGEMENT,
            RETENTION_MANAGEMENT,
            VERSION_MANAGEMENT,
            READ_ACCESS_CONTROL,
            MODIFY_ACCESS_CONTROL,
            READ, WRITE);
    
    private static final long serialVersionUID = 1L;
    private String principal;
    
    private JcrPermission[] PERMISSIONS = new JcrPermission[]{
        ALL,
        LIFECYCLE_MANAGEMENT,
        LOCK_MANAGEMENT,
        NODE_TYPE_MANAGEMENT,
        RETENTION_MANAGEMENT,
        VERSION_MANAGEMENT,
        READ_ACCESS_CONTROL,
        MODIFY_ACCESS_CONTROL,
        READ,
        WRITE,
        ADD_CHILD_NODES,
        MODIFY_PROPERTIES,
        REMOVE_CHILD_NODES
    };

    public String getPrincipal() {
        return principal;
    }
    
    public void setPrincipal(String principal) {
        this.principal = principal;
    }
    
    public void enable( String name ) {
        forName(name).setStatus(true);
    }
    
    public void disable( String name ) {
        forName(name).setStatus(false);
    }
    
    public void disableAll() {
        for (int i = 0; i < PERMISSIONS.length; i++) {
            PERMISSIONS[i].setStatus(false);
        }
    }

    public void enableAll() {
        for (int i = 0; i < PERMISSIONS.length; i++) {
            PERMISSIONS[i].setStatus(true);
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<JcrPermission> permissions() {
        ArrayList<JcrPermission> list = new ArrayList();
        for (int i = 0; i < PERMISSIONS.length; i++) {
            list.add(PERMISSIONS[i]);
        }
        return list;
    }
    
    public void modify(JcrPermission permission, boolean enabled) {
        permission.setStatus(enabled);
    }

    private JcrPermission forName(String name) {
        for (int i = 0; i < PERMISSIONS.length; i++) {
            if (PERMISSIONS[i].getName().equalsIgnoreCase(name)) {
                return PERMISSIONS[i];
            }
        }
        return null;
    }
    
    public boolean hasPermission(JcrPermission permission) {
        for (JcrPermission p : PERMISSIONS) {
            if (p.matches(permission)) {
                return true;
            }
        }
        return false;
    }
}
