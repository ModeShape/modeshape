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
 *
 * @author kulikov
 */
public class Policy implements Serializable {
    private String principal;
    private ArrayList<JcrPermission> permissions = new ArrayList();
    
    public String getPrincipal() {
        return principal;
    }
    
    public void setPrincipal(String principal) {
        this.principal = principal;
    }
    
    public void add(JcrPermission permission) {
        permissions.add(permission);
    }
    
    public Collection<JcrPermission> permissions() {
        return permissions;
    }
    
    public void modify(JcrPermission permission, boolean enabled) {
        if (enabled) {
            permissions.add(permission);
        } else {
            permissions.remove(permission);
        }
    }
    
    public boolean hasPermission(JcrPermission permission) {
        for (JcrPermission p : permissions) {
            if (p.matches(permission)) {
                return true;
            }
        }
        return false;
    }
}
