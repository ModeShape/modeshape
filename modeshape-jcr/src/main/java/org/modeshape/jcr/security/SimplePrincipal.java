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
