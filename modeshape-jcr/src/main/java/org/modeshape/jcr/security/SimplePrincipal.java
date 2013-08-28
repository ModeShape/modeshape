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
package org.modeshape.jcr.security;

import java.security.Principal;

/**
 * Default local implementation of the principal.
 * 
 * @author kulikov
 */
public final class SimplePrincipal implements Principal {

    private static final String EVERYONE_PRINCIPAL_NAME = "everyone";
    
    //The name of the user
    private final String name;
    
    //Principal that equals to any other principal
    public static final SimplePrincipal EVERYONE = SimplePrincipal.newInstance(EVERYONE_PRINCIPAL_NAME);
    
    /**
     * Creates new instance of the principal.
     * 
     * @param name the name of the user.
     * @return new user name principal.
     */
    public static SimplePrincipal newInstance(String name) {
        return new SimplePrincipal(name);
    }
    
    /**
     * Constructs new instance.
     * 
     * @param name the name of the user
     */
    private SimplePrincipal(String name) {
        assert(name != null);
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Principal) {
            if (other == this) {
                return true;
            }
            if (this.getName().equals(EVERYONE_PRINCIPAL_NAME)) {
                return true;
            }
            Principal that = (Principal) other;
            return this.getName().equals(that.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
