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
