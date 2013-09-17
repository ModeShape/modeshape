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
public class JcrAccessControlList implements Serializable {
    private ArrayList<JcrACLEntry> entries = new ArrayList();
    
    public JcrAccessControlList() {
    }
    
    public void add(JcrACLEntry entry) {
        entries.add(entry);
    }
    
    public void remove(JcrACLEntry entry) {
        entries.remove(entry);
    }
    
    public Collection<JcrACLEntry> entries() {
        return entries;
    }
}
