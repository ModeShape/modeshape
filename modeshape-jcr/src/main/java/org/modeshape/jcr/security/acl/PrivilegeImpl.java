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
package org.modeshape.jcr.security.acl;

import java.util.ArrayList;
import javax.jcr.Session;
import javax.jcr.security.Privilege;
import org.modeshape.common.annotation.Immutable;

/**
 * Implements JCR privilege object.  
 * 
 * Privilege object represents one or more permission.
 * 
 * @author kulikov
 */
@Immutable
public class PrivilegeImpl implements Privilege {
    private final static String URI = "http://www.jcp.org/jcr/1.0";

    //The name of this privilege.    
    private String name;
    
    //List of privileges directly contained by this privilege
    private Privilege[] declaredPrivileges;
    
    //abstract flag
    private boolean isAbstract = false;
    
    private Session session;
    
    /**
     * Creates new instance of the privilege object.
     * 
     * @param session session under access control
     * @param name the name of privilege.
     * @param declaredPrivileges list of privileges aggregated by this object.
     */
    public PrivilegeImpl(Session session, String name, Privilege[] declaredPrivileges) {
        this.name = name.substring(name.indexOf('}') + 1);
        this.declaredPrivileges = declaredPrivileges;
        this.session = session;
    }

    /**
     * Creates new instance of the privilege object.
     * 
     * @param session the session under control
     * @param name the name of privilege.
     * @param declaredPrivileges list of privileges aggregated by this object.
     * @param isAbstract true if this is abstract privilege.
     */
    public PrivilegeImpl(Session session, String name, Privilege[] declaredPrivileges, boolean isAbstract) {
        this.session = session;
        this.name = name.substring(name.indexOf('}') + 1);
        this.declaredPrivileges = declaredPrivileges;
        this.isAbstract = isAbstract;
    }
    
    /**
     * The name without prefix.
     * 
     * @return name of privilege without prefix
     */
    public String localName() {
        return name;
    }
    
    @Override
    public String getName() {
        try {
            return session.getNamespacePrefix(URI) + ":" + name;
        } catch (Exception e) {
            //will never happen
            return null;
        }
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public boolean isAggregate() {
        return this.declaredPrivileges != null && declaredPrivileges.length > 0;
    }

    @Override
    public Privilege[] getDeclaredAggregatePrivileges() {
        return this.declaredPrivileges;
    }

    @Override
    public Privilege[] getAggregatePrivileges() {
        ArrayList<Privilege> list = new ArrayList<Privilege>();
        for (Privilege ap : getDeclaredAggregatePrivileges()) {
            aggregate(list, ap);
        }
        
        Privilege[] res = new Privilege[list.size()];
        list.toArray(res);
        return res;
    }
    
    /**
     * Recursively aggregates privileges for the given privilege.
     * 
     * @param list list which holds all aggregate privileges.
     * @param p the given privilege
     */
    private void aggregate(ArrayList<Privilege> list, Privilege p) {
        list.add(p);
        if (p.isAggregate()) {
            for (Privilege ap : p.getDeclaredAggregatePrivileges()) {
                aggregate(list, ap);
            }
        }
    }
    
    /**
     * Tests given privilege.
     * 
     * @param p the given privilege.
     * @return true if this privilege equals or aggregates given privilege.
     * 
     */
    public boolean contains(Privilege p) {
        if (p.getName().equalsIgnoreCase(this.getName())) {
            return true;
        }
        
        Privilege[] list = getAggregatePrivileges();
        for (Privilege privilege : list) {
            if (privilege.getName().equalsIgnoreCase(p.getName())) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        
        if (!(other instanceof Privilege)) {
            return false;
        }
        
        return ((Privilege)other).getName().equals(this.getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
