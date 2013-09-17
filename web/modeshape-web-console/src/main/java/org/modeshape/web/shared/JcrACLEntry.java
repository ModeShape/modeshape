/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author kulikov
 */
public class JcrACLEntry implements Serializable {
    private String principal;
    private ArrayList<JcrPermission> permissions = new ArrayList();
    
    public JcrACLEntry() {
    }
    
    public String getPrincipal() {
        return principal;
    }
    
    public void setPrincipal(String principal) {
        this.principal = principal;
    }
    
    public void add(JcrPermission permission) {
        permissions.add(permission);
    }
    
    public void remove(JcrPermission permission) {
        permissions.remove(permission);
    }
    
    public Collection getPermissions() {
        return permissions;
    }
}
