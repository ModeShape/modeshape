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
