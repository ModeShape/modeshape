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
public class JcrRepositoryDescriptor implements Serializable {
    private ArrayList<Param> info = new ArrayList();
    
    public JcrRepositoryDescriptor()  {
        
    }

    public void add(String name, String value) {
        info.add(new Param(name, value));
    }
    
    public Collection<Param> info() {
        return info;
    }
    
}
