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

/**
 * @author kulikov
 */
public class JcrPolicy implements Serializable {
    private static final long serialVersionUID = 1L;
    private String principal;
    private ArrayList<JcrPermission> permissions = new ArrayList<JcrPermission>();

    public static JcrPolicy everyone() {
        return new JcrPolicy("EVERYONE");
    }

    public JcrPolicy() {
    }

    /**
     * Creates ACL entry for the given principal and granting all permissions.
     * 
     * @param principal the name of the principal.
     */
    public JcrPolicy( String principal ) {
        this.principal = principal;
        this.permissions.add(JcrPermission.ALL);
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal( String principal ) {
        this.principal = principal;
    }

    public void add( JcrPermission permission ) {
        permissions.add(permission);
    }

    public void remove( JcrPermission permission ) {
        permissions.remove(permission);
    }

    public Collection<JcrPermission> getPermissions() {
        return permissions;
    }

    public void update( String action,
                        String value ) {
        Boolean enable = value.equals("Allow");
        if (enable) {
            permissions.add(JcrPermission.fromDisplayName(action));
        } else {
            JcrPermission p = find(action);
            permissions.remove(p);
        }
    }

    private JcrPermission find( String name ) {
        JcrPermission permission = JcrPermission.fromDisplayName(name);
        for (JcrPermission p : permissions) {
            if (permission.getName().equals(p.getName())) {
                return p;
            }
        }
        return null;
    }

}
