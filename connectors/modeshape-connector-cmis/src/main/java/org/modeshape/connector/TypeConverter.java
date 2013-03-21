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
package org.modeshape.connector;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.*;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;

/**
 * Converts types between CMIS repository and Modeshape repository.
 * 
 * @author kulikov
 */
public class TypeConverter {
    public void define(List<Tree<ObjectType>> types, NodeTypeManager typeManager) throws Exception {
        for (int i = 0; i < types.size(); i++) {
            Tree<ObjectType> tree = types.get(i);
            define(tree.getItem(), typeManager);
            define(tree.getChildren(), typeManager);
        }
    }

    public void define(ObjectType cmisType, NodeTypeManager typeManager) throws Exception {
        //skip base types cmis:folder and cmis:document.
        //we will map those types to jcr types explicit
        if (cmisType.isBaseType()) {
            return;
        }
        NodeTypeTemplate type = typeManager.createNodeTypeTemplate();
System.out.println("+++++CMIS type=" + cmisType.getId());
        type.setName(cmisType.getId());
        type.setAbstract(false);
        type.setMixin(true);
        type.setOrderableChildNodes(true);
        type.setQueryable(true);

        if (cmisType.getBaseTypeId() == BaseTypeId.CMIS_FOLDER) {
            type.setDeclaredSuperTypeNames(new String[]{"nt:folder"});
        } else if (cmisType.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT) {
            type.setDeclaredSuperTypeNames(new String[]{"nt:file"});
        } else {
            type.setDeclaredSuperTypeNames(new String[]{cmisType.getBaseTypeId().name()});
        }
        

        Map<String, PropertyDefinition<?>> props = cmisType.getPropertyDefinitions();
        Set<String> names = props.keySet();

        for (String name : names) {
System.out.println("---CMIS property=" + name);
            PropertyDefinition pd = props.get(name);
            PropertyDefinitionTemplate pt = typeManager.createPropertyDefinitionTemplate();

            pt.setAutoCreated(false);
            pt.setAvailableQueryOperators(new String[]{});
            pt.setName(name);
            pt.setMandatory(pd.isRequired());

            type.getPropertyDefinitionTemplates().add(pt);
        }

        Iterator<ObjectType> children = cmisType.getChildren().iterator();
        while (children.hasNext()) {
            ObjectType child = children.next();
            NodeDefinitionTemplate ct = typeManager.createNodeDefinitionTemplate();

            ct.setAutoCreated(false);
            ct.setName(child.getId());

            type.getNodeDefinitionTemplates().add(ct);
        }

        NodeTypeDefinition[] nodeDefs = new NodeTypeDefinition[] {type};
        typeManager.registerNodeTypes(nodeDefs, true);
    }
}
