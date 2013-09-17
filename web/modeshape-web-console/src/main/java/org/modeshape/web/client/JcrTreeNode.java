/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import com.smartgwt.client.widgets.tree.TreeNode;
import java.util.Collection;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrProperty;

/**
 *
 * @author kulikov
 */
public class JcrTreeNode extends TreeNode {
    
    private String primaryType;
    private Collection<JcrProperty> properties;
    private JcrAccessControlList acl;
    /**
     * 
     * @param name
     * @param path 
     */
    public JcrTreeNode(String name, String path, String primaryType) {
        super();
        setName(name);
        setAttribute("path", path);
        this.primaryType = primaryType;
    }
    
    public JcrTreeNode(String name, String path, JcrTreeNode... children) {
        super();
        setName(name);
        setAttribute("path", path);
        setChildren(children);
    }
    
    public String getPrimaryType() {
        return primaryType;
    }
    
    public void setProperties(Collection<JcrProperty> properties) {
        this.properties = properties;
    }
    
    public Collection<JcrProperty> getProperties() {
        return properties;
    }
    
    public JcrAccessControlList getAccessList() {
        return acl;
    }
    
    public void setAcessControlList(JcrAccessControlList acl) {
        this.acl = acl;
    }
    
}
