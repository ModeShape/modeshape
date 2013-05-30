/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.jcr.security.acl;

import java.util.ArrayList;
import javax.jcr.security.Privilege;
import org.modeshape.common.annotation.Immutable;

/**
 * Implements JCR privilege object.  
 * 
 * @author kulikov
 */
@Immutable
public class PrivilegeImpl implements Privilege {

    //The name of this privilege.    
    private String name;
    
    //List of privileges directly contained by this privilege
    private Privilege[] declaredPrivileges;
    
    //abstract flag
    private boolean isAbstract = false;
    
    /**
     * Creates new instance of the privilege object.
     * 
     * @param name the name of privilege.
     * @param declaredPrivileges list of privileges aggregated by this object.
     */
    public PrivilegeImpl(String name, Privilege[] declaredPrivileges) {
        this.name = name;
        this.declaredPrivileges = declaredPrivileges;
    }

    /**
     * Creates new instance of the privilege object.
     * 
     * @param name the name of privilege.
     * @param declaredPrivileges list of privileges aggregated by this object.
     * @boolean isAbstract true if this is abstract privilege.
     */
    public PrivilegeImpl(String name, Privilege[] declaredPrivileges, boolean isAbstract) {
        this.name = name;
        this.declaredPrivileges = declaredPrivileges;
        this.isAbstract = isAbstract;
    }
    
    @Override
    public String getName() {
        return name;
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
        ArrayList<Privilege> list = new ArrayList();
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
}
