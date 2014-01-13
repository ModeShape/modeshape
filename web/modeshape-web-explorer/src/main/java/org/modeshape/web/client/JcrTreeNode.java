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
package org.modeshape.web.client;

import java.util.Collection;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrProperty;
import com.smartgwt.client.widgets.tree.TreeNode;

/**
 * @author kulikov
 */
public class JcrTreeNode extends TreeNode {

    private String primaryType;
    private Collection<JcrProperty> properties;
    private JcrAccessControlList acl;
    private String[] mixins;
    private String[] propertyDefs;

    public JcrTreeNode( String name,
                        String path,
                        String primaryType ) {
        super();
        setName(name);
        setAttribute("path", path);
        this.primaryType = primaryType;
    }

    public JcrTreeNode( String name,
                        String path,
                        JcrTreeNode... children ) {
        super();
        setName(name);
        setAttribute("path", path);
        setChildren(children);
    }

    public String getPath() {
        return getAttribute("path");
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public void setProperties( Collection<JcrProperty> properties ) {
        this.properties = properties;
    }

    public Collection<JcrProperty> getProperties() {
        return properties;
    }

    public JcrAccessControlList getAccessList() {
        return acl;
    }

    public void setAcessControlList( JcrAccessControlList acl ) {
        this.acl = acl;
    }

    public void setMixins( String[] mixins ) {
        this.mixins = mixins;
    }

    public String[] getMixins() {
        return mixins;
    }

    public void setPropertyDefs( String[] propertyDefs ) {
        this.propertyDefs = propertyDefs;
    }

    public String[] getPropertyDefs() {
        return propertyDefs;
    }

}
