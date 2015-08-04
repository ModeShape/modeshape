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
import javax.jcr.security.Privilege;

/**
 * Permission object.
 * 
 * @author kulikov
 */
public class JcrPermission implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String displayName;
    private String jcrName;
    private boolean status;
    
    private ArrayList<JcrPermission> aggregates = new ArrayList<JcrPermission>();
    
    public static final JcrPermission LIFECYCLE_MANAGEMENT = new JcrPermission(Privilege.JCR_LIFECYCLE_MANAGEMENT, "Life cycle management");
    public static final JcrPermission LOCK_MANAGEMENT = new JcrPermission(Privilege.JCR_LOCK_MANAGEMENT, "Lock management");
    public static final JcrPermission NODE_TYPE_MANAGEMENT = new JcrPermission(Privilege.JCR_NODE_TYPE_MANAGEMENT, "Node type management");
    public static final JcrPermission RETENTION_MANAGEMENT = new JcrPermission(Privilege.JCR_RETENTION_MANAGEMENT, "Retention management");
    public static final JcrPermission VERSION_MANAGEMENT = new JcrPermission(Privilege.JCR_VERSION_MANAGEMENT, "Version management");
    
    public static final JcrPermission READ_ACCESS_CONTROL = new JcrPermission(Privilege.JCR_READ_ACCESS_CONTROL, "Read access control");
    public static final JcrPermission MODIFY_ACCESS_CONTROL = new JcrPermission(Privilege.JCR_MODIFY_ACCESS_CONTROL, "Modify access control");

    public static final JcrPermission READ = new JcrPermission(Privilege.JCR_READ, "Read");
    
    public static final JcrPermission ADD_CHILD_NODES = new JcrPermission(Privilege.JCR_ADD_CHILD_NODES, "Add child nodes");
    public static final JcrPermission REMOVE_CHILD_NODES = new JcrPermission(Privilege.JCR_REMOVE_CHILD_NODES, "Remove child nodes");
    public static final JcrPermission MODIFY_PROPERTIES = new JcrPermission(Privilege.JCR_MODIFY_PROPERTIES, "Modify properties");
    public static final JcrPermission WRITE = new JcrPermission(Privilege.JCR_WRITE, "Write",
            ADD_CHILD_NODES, REMOVE_CHILD_NODES, MODIFY_PROPERTIES);

    public static final JcrPermission ALL = new JcrPermission(Privilege.JCR_ALL, "All permissions",
            LIFECYCLE_MANAGEMENT, 
            LOCK_MANAGEMENT, 
            NODE_TYPE_MANAGEMENT,
            RETENTION_MANAGEMENT,
            VERSION_MANAGEMENT,
            READ_ACCESS_CONTROL,
            MODIFY_ACCESS_CONTROL,
            READ, WRITE);
    

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
    
    public static JcrPermission fromDisplayName(String name) {
        for (int i = 0; i < PERMISSIONS.length; i++) {
            if (PERMISSIONS[i].getDisplayName().equals(name)) {
                return PERMISSIONS[i];
            }
        }
        return null;
    }

    public static JcrPermission forName(String name) {
        for (int i = 0; i < PERMISSIONS.length; i++) {
            if (PERMISSIONS[i].getName().equalsIgnoreCase(name)) {
                return PERMISSIONS[i];
            }
        }
        return null;
    }
    
    public JcrPermission() {
    }
    
    protected JcrPermission(String name) {
        if (name.startsWith("{")) {
            this.name = "jcr:" + name.substring(name.indexOf("}") + 1);
            this.jcrName = name;
        } else {
            this.name = name;
        }
    }
    
    protected JcrPermission(String name, String displayName) {
        this(name);
        this.displayName = displayName;
    }
    
    protected JcrPermission(String name, String displayName, JcrPermission... aggregates) {
        this(name);
        this.displayName = displayName;
        this.aggregates.addAll(Arrays.asList(aggregates));
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getJcrName() {
        return jcrName;
    }
    
    public boolean getStatus() {
        return status;
    }
    
    public void setStatus(boolean status) {
        this.status = status;
    }
    
    public boolean matches(JcrPermission permission) {
        if (this.name.equalsIgnoreCase(permission.name)) {
            return true;
        }
        
        for (JcrPermission p : aggregates) {
            if (p.matches(permission)) {
                return true;
            }
        }
        
        return false;
    }
}
