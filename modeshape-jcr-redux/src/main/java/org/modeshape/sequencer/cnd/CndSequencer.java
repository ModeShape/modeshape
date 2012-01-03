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
package org.modeshape.sequencer.cnd;


import javax.jcr.*;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.CndImporter;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.jcr.core.ExecutionContext;
import static org.modeshape.sequencer.cnd.CndSequencerLexicon.*;
import org.modeshape.jcr.value.NamespaceRegistry.Namespace;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * A sequencer of CND files.
 *
 * @author Horia Chiorean
 */
public class CndSequencer extends Sequencer {

    private static final String RESIDUAL_ITEM_NAME = "*";
    private static final Logger LOGGER = Logger.getLogger(CndSequencer.class);

    @Override
    public void initialize( NamespaceRegistry registry, NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("sequencer-cnd.cnd", nodeTypeManager, true);
    }

    @Override
    public boolean execute( Property inputProperty, Node outputNode, Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        //if the output is jcr:system/jcr:nodeTypes import via the NodeTypeManager
        if (outputIsSystemNodeTypes(outputNode, binaryValue)) {
            return true;
        }

        CndImporter cndImporter = importNodesFromCND(binaryValue.getStream());
        if (cndImporter == null) {
            return false;
        }

        registerImportedNamespaces(outputNode, cndImporter);
        processNodeTypeDefinitions(outputNode, cndImporter);

        return true;
    }

    private void processNodeTypeDefinitions( Node outputNode, CndImporter cndImporter ) throws RepositoryException {
        List<NodeTypeDefinition> importedNodeTypes = cndImporter.getNodeTypeDefinitions();

        for (NodeTypeDefinition nodeTypeDefinition : importedNodeTypes) {
            storeNodeTypeDefinition(outputNode, nodeTypeDefinition);
        }
    }

    private void registerImportedNamespaces( Node outputNode, CndImporter cndImporter ) throws RepositoryException {
        NamespaceRegistry namespaceRegistry = outputNode.getSession().getWorkspace().getNamespaceRegistry();
        for (Namespace namespace : cndImporter.getNamespaces()) {
            super.registerNamespace(namespace.getPrefix(), namespace.getNamespaceUri(), namespaceRegistry);
        }
    }

    private CndImporter importNodesFromCND( InputStream cndInputStream ) throws IOException {
        Problems problemsDuringImport = new SimpleProblems();

        CndImporter cndImporter = new CndImporter(new ExecutionContext(), true);
        cndImporter.importFrom(cndInputStream, problemsDuringImport, null);
        if (problemsDuringImport.hasErrors()) {
            problemsDuringImport.writeTo(LOGGER);
            return null;
        }

        return cndImporter;
    }

    private boolean outputIsSystemNodeTypes( Node outputNode, Binary binaryValue ) throws RepositoryException, IOException {
        String systemNodesPath = JcrLexicon.SYSTEM.getString() + "/" + JcrLexicon.NODE_TYPES.getString();
        if (outputNode.getPath().contains(systemNodesPath)) {
            NodeTypeManager nodeTypeManager = (NodeTypeManager)outputNode.getSession().getWorkspace().getNodeTypeManager();
            nodeTypeManager.registerNodeTypes(binaryValue.getStream(), true);
            return true;
        }
        return false;
    }

    private void storeNodeTypeDefinition( Node outputNode, NodeTypeDefinition nodeTypeDefinition ) throws RepositoryException {
        Node nodeTypeNode = processNodeTypeDefinition(outputNode, nodeTypeDefinition);

        PropertyDefinition[] declaredPropertyDefinitions = nodeTypeDefinition.getDeclaredPropertyDefinitions();
        if (declaredPropertyDefinitions != null) {
            for (PropertyDefinition propertyDefinition : declaredPropertyDefinitions) {
                processPropertyDefinition(nodeTypeNode, propertyDefinition);
            }
        }

        NodeDefinition[] declaredChildNodeDefinitions = nodeTypeDefinition.getDeclaredChildNodeDefinitions();
        if (declaredChildNodeDefinitions != null) {
            for (NodeDefinition childNodeDefinition : declaredChildNodeDefinitions) {
                processChildNodeDefinition(nodeTypeNode, childNodeDefinition);
            }
        }
    }

    private void processChildNodeDefinition( Node nodeTypeNode, NodeDefinition childNodeDefinition ) throws RepositoryException {
        Node childNode = nodeTypeNode.addNode(CHILD_NODE_DEFINITION, CHILD_NODE_DEFINITION);

        if (!RESIDUAL_ITEM_NAME.equals(childNodeDefinition.getName())) {
            childNode.setProperty(NAME, childNodeDefinition.getName());
        }

        childNode.setProperty(AUTO_CREATED, childNodeDefinition.isAutoCreated());
        childNode.setProperty(MANDATORY, childNodeDefinition.isMandatory());
        childNode.setProperty(ON_PARENT_VERSION, OnParentVersionAction.nameFromValue(childNodeDefinition.getOnParentVersion()));
        childNode.setProperty(PROTECTED, childNodeDefinition.isProtected());
        String[] requiredPrimaryTypeNames = childNodeDefinition.getRequiredPrimaryTypeNames();
        childNode.setProperty(REQUIRED_PRIMARY_TYPES, requiredPrimaryTypeNames != null ? requiredPrimaryTypeNames : new String[0]);
        childNode.setProperty(SAME_NAME_SIBLINGS, childNodeDefinition.allowsSameNameSiblings());
        childNode.setProperty(DEFAULT_PRIMARY_TYPE, childNodeDefinition.getDefaultPrimaryTypeName());
    }

    private void processPropertyDefinition( Node nodeTypeNode, PropertyDefinition propertyDefinition ) throws RepositoryException {
        Node propertyDefinitionNode = nodeTypeNode.addNode(PROPERTY_DEFINITION, PROPERTY_DEFINITION);

        if (!RESIDUAL_ITEM_NAME.equals(propertyDefinition.getName())) {
            propertyDefinitionNode.setProperty(NAME, propertyDefinition.getName());
        }
        propertyDefinitionNode.setProperty(AUTO_CREATED, propertyDefinition.isAutoCreated());
        propertyDefinitionNode.setProperty(MANDATORY, propertyDefinition.isMandatory());
        propertyDefinitionNode.setProperty(MULTIPLE, propertyDefinition.isMultiple());
        propertyDefinitionNode.setProperty(PROTECTED, propertyDefinition.isProtected());
        propertyDefinitionNode.setProperty(ON_PARENT_VERSION, OnParentVersionAction.nameFromValue(propertyDefinition.getOnParentVersion()));
        propertyDefinitionNode.setProperty(REQUIRED_TYPE, PropertyType.nameFromValue(propertyDefinition.getRequiredType()).toUpperCase());
        String[] availableQueryOperators = propertyDefinition.getAvailableQueryOperators();       
        propertyDefinitionNode.setProperty(AVAILABLE_QUERY_OPERATORS, availableQueryOperators != null ? availableQueryOperators : new String[0]);
        Value[] defaultValues = propertyDefinition.getDefaultValues();
        propertyDefinitionNode.setProperty(DEFAULT_VALUES, defaultValues != null ? defaultValues : new Value[0]);
        String[] valueConstraints = propertyDefinition.getValueConstraints();
        propertyDefinitionNode.setProperty(VALUE_CONSTRAINTS, valueConstraints != null ? valueConstraints : new String[0]);
        propertyDefinitionNode.setProperty(IS_FULL_TEXT_SEARCHABLE, propertyDefinition.isFullTextSearchable());
        propertyDefinitionNode.setProperty(IS_QUERY_ORDERABLE, propertyDefinition.isQueryOrderable());
    }

    private Node processNodeTypeDefinition( Node outputNode, NodeTypeDefinition nodeTypeDefinition ) throws RepositoryException {
        Node nodeTypeNode = outputNode.addNode(nodeTypeDefinition.getName(), NODE_TYPE);

        nodeTypeNode.setProperty(IS_MIXIN, nodeTypeDefinition.isMixin());
        nodeTypeNode.setProperty(IS_ABSTRACT, nodeTypeDefinition.isAbstract());
        nodeTypeNode.setProperty(IS_QUERYABLE, nodeTypeDefinition.isQueryable());
        nodeTypeNode.setProperty(NODE_TYPE_NAME, nodeTypeDefinition.getName());
        nodeTypeNode.setProperty(HAS_ORDERABLE_CHILD_NODES, nodeTypeDefinition.hasOrderableChildNodes());
        String[] declaredSupertypeNames = nodeTypeDefinition.getDeclaredSupertypeNames();
        nodeTypeNode.setProperty(SUPERTYPES, declaredSupertypeNames != null ? declaredSupertypeNames : new String[0]);

        if (nodeTypeDefinition.getPrimaryItemName() != null) {
            nodeTypeNode.setProperty(PRIMARY_ITEM_NAME, nodeTypeDefinition.getPrimaryItemName());
        }

        return nodeTypeNode;
    }
}
