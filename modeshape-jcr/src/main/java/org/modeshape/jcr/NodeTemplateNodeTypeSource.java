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
package org.modeshape.jcr;

import java.util.Arrays;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.OnParentVersionAction;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.io.GraphBatchDestination;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.jcr.nodetype.InvalidNodeTypeDefinitionException;
import org.modeshape.jcr.nodetype.NodeTypeTemplate;

/**
 * Class to convert one or more {@link NodeTypeTemplate node type templates} containing custom node type definitions into a format
 * that can be registered with the {@link JcrNodeTypeManager}.
 * <p>
 * As the JSR-283 specification mandates that node type templates be the standard basis for custom type registration, the
 * {@link RepositoryNodeTypeManager#registerNodeTypes(java.util.Collection, boolean)} method should be used in preference to
 * manually instantiating this class.
 * </p>
 */
@NotThreadSafe
class NodeTemplateNodeTypeSource implements JcrNodeTypeSource {

    private final Graph graph;
    private final PathFactory pathFactory;
    private final NameFactory nameFactory;
    private final ValueFactory<Boolean> booleanFactory;
    private final ValueFactory<String> stringFactory;
    private final Destination destination;

    public NodeTemplateNodeTypeSource( NodeTypeTemplate nodeTypeTemplate ) throws InvalidNodeTypeDefinitionException {
        this(Arrays.asList(new NodeTypeTemplate[] {nodeTypeTemplate}));
    }

    public NodeTemplateNodeTypeSource( List<NodeTypeTemplate> nodeTypeTemplates ) throws InvalidNodeTypeDefinitionException {

        ExecutionContext context = null;

        if (nodeTypeTemplates.isEmpty()) {
            context = new ExecutionContext();
        } else {
            for (NodeTypeTemplate ntt : nodeTypeTemplates) {
                if (!(ntt instanceof JcrNodeTypeTemplate)) {
                    throw new IllegalArgumentException(JcrI18n.cannotConvertValue.text(ntt.getClass(), JcrNodeTypeTemplate.class));
                }

                JcrNodeTypeTemplate jntt = (JcrNodeTypeTemplate)ntt;
                if (context == null) {
                    context = jntt.getExecutionContext();
                    assert context != null;
                } else {
                    if (context != jntt.getExecutionContext()) {
                        throw new IllegalArgumentException(JcrI18n.allNodeTypeTemplatesMustComeFromSameSession.text());
                    }
                }
            }
        }

        assert context != null;
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.nameFactory = context.getValueFactories().getNameFactory();
        this.booleanFactory = context.getValueFactories().getBooleanFactory();
        this.stringFactory = context.getValueFactories().getStringFactory();

        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("NodeTypeTemplate Import Source");
        this.graph = Graph.create(source, context);
        Graph.Batch batch = graph.batch();
        destination = new GraphBatchDestination(batch);

        Path rootPath = pathFactory.createRootPath();
        for (NodeTypeTemplate template : nodeTypeTemplates) {
            this.createNodeType((JcrNodeTypeTemplate)template, rootPath);
        }

        destination.submit();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.JcrNodeTypeSource#getNodeTypes()
     */
    public final Graph getNodeTypes() {
        return graph;
    }

    private boolean booleanFrom( Object value,
                                 boolean defaultValue ) {
        if (value == null) return defaultValue;

        return booleanFactory.create(value);
    }

    private Name nameFrom( Object value ) {
        return nameFactory.create(value);
    }

    private Name[] namesFrom( Object[] values ) {
        if (values == null) return new Name[0];

        Name[] names = new Name[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = nameFactory.create(values[i]);
        }

        return names;
    }

    private String[] stringsFrom( Object[] values ) {
        if (values == null) return new String[0];

        String[] strings = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strings[i] = stringFactory.create(values[i]);
        }

        return strings;
    }

    /**
     * Project the custom node type definition from the given template onto the {@link #getNodeTypes() graph}.
     * 
     * @param nodeType
     * @param parentPath
     * @return the path to the newly created node
     * @throws InvalidNodeTypeDefinitionException
     */
    @SuppressWarnings( "deprecation" )
    protected Path createNodeType( JcrNodeTypeTemplate nodeType,
                                   Path parentPath ) throws InvalidNodeTypeDefinitionException {

        Name name = nameFrom(nodeType.getName());
        Name[] supertypes = namesFrom(nodeType.getDeclaredSupertypes());
        boolean isAbstract = booleanFrom(nodeType.isAbstract(), false);
        boolean hasOrderableChildNodes = booleanFrom(nodeType.hasOrderableChildNodes(), false);
        boolean isMixin = booleanFrom(nodeType.isMixin(), false);
        boolean isQueryable = true;
        Name primaryItemName = nameFrom(nodeType.getPrimaryItemName());

        // Create the node for the node type ...
        if (name == null) throw new InvalidNodeTypeDefinitionException(JcrI18n.invalidNodeTypeName.text());
        Path path = pathFactory.create(parentPath, name);

        PropertyFactory factory = nodeType.getExecutionContext().getPropertyFactory();
        destination.create(path,
                           factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.NODE_TYPE),
                           factory.create(JcrLexicon.SUPERTYPES, (Object[])supertypes),
                           factory.create(JcrLexicon.IS_ABSTRACT, isAbstract),
                           factory.create(JcrLexicon.HAS_ORDERABLE_CHILD_NODES, hasOrderableChildNodes),
                           factory.create(JcrLexicon.IS_MIXIN, isMixin),
                           factory.create(JcrLexicon.IS_QUERYABLE, isQueryable),
                           factory.create(JcrLexicon.NODE_TYPE_NAME, name),
                           factory.create(JcrLexicon.PRIMARY_ITEM_NAME, primaryItemName));

        for (PropertyDefinition propDefn : nodeType.getPropertyDefinitionTemplates()) {
            createPropertyDefinition((JcrPropertyDefinitionTemplate)propDefn, path);
        }

        for (NodeDefinition nodeDefn : nodeType.getNodeDefinitionTemplates()) {
            createChildDefinition((JcrNodeDefinitionTemplate)nodeDefn, path);
        }

        return path;
    }

    /**
     * Project the property definition from the given template onto the {@link #getNodeTypes() graph}.
     * 
     * @param propDefn
     * @param parentPath
     * @return the path to the newly created node
     */
    protected Path createPropertyDefinition( JcrPropertyDefinitionTemplate propDefn,
                                             Path parentPath ) {
        Name name = nameFrom(propDefn.getName());
        String requiredType = PropertyType.nameFromValue(propDefn.getRequiredType()).toUpperCase();
        Value[] rawValues = propDefn.getDefaultValues();
        boolean multiple = booleanFrom(propDefn.isMultiple(), false);
        boolean mandatory = booleanFrom(propDefn.isMandatory(), false);
        boolean autoCreated = booleanFrom(propDefn.isAutoCreated(), false);
        boolean isProtected = booleanFrom(propDefn.isProtected(), false);
        String onParentVersion = OnParentVersionAction.nameFromValue(propDefn.getOnParentVersion()).toUpperCase();
        // /*QueryOperator[] queryOperators =*/queryOperatorsFrom(propDefn, CndLexer.QUERY_OPERATORS);
        // boolean isFullTextSearchable = booleanFrom(propDefn, CndLexer.IS_FULL_TEXT_SEARCHABLE, true);
        // boolean isQueryOrderable = booleanFrom(propDefn, CndLexer.IS_QUERY_ORDERERABLE, true);
        String[] valueConstraints = stringsFrom(propDefn.getValueConstraints());

        // Create the node for the node type ...
        if (name == null) name = JcrNodeType.RESIDUAL_NAME;
        Path path = pathFactory.create(parentPath, JcrLexicon.PROPERTY_DEFINITION);

        String defaultValues[];

        if (rawValues == null) {
            defaultValues = new String[0];
        } else {
            try {
                defaultValues = new String[rawValues.length];
                for (int i = 0; i < rawValues.length; i++) {
                    defaultValues[i] = rawValues[i].getString();
                }
            } catch (RepositoryException re) {
                throw new IllegalStateException(re);
            }
        }

        PropertyFactory factory = propDefn.getExecutionContext().getPropertyFactory();
        destination.create(path,
                           factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.PROPERTY_DEFINITION),
                           factory.create(JcrLexicon.REQUIRED_TYPE, requiredType),
                           factory.create(JcrLexicon.DEFAULT_VALUES, defaultValues),
                           factory.create(JcrLexicon.MULTIPLE, multiple),
                           factory.create(JcrLexicon.MANDATORY, mandatory),
                           factory.create(JcrLexicon.NAME, name),
                           factory.create(JcrLexicon.AUTO_CREATED, autoCreated),
                           factory.create(JcrLexicon.PROTECTED, isProtected),
                           factory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion),
                           // factory.create(ModeShapeLexicon.QUERY_OPERATORS, queryOperators),
                           // factory.create(JcrLexicon.IS_FULL_TEXT_SEARCHABLE, isFullTextSearchable),
                           // factory.create(JcrLexicon.IS_QUERY_ORDERABLE, isQueryOrderable),
                           factory.create(JcrLexicon.VALUE_CONSTRAINTS, (Object[])valueConstraints));

        return path;
    }

    /**
     * Project the child node definition from the given template onto the {@link #getNodeTypes() graph}.
     * 
     * @param childDefn
     * @param parentPath
     * @return the path to the newly created node
     */
    protected Path createChildDefinition( JcrNodeDefinitionTemplate childDefn,
                                          Path parentPath ) {
        Name name = nameFrom(childDefn.getName());
        Name[] requiredPrimaryTypes = namesFrom(childDefn.getRequiredPrimaryTypeNames());
        Name defaultPrimaryType = nameFrom(childDefn.getDefaultPrimaryTypeName());
        boolean mandatory = booleanFrom(childDefn.isMandatory(), false);
        boolean autoCreated = booleanFrom(childDefn.isAutoCreated(), false);
        boolean isProtected = booleanFrom(childDefn.isProtected(), false);
        String onParentVersion = OnParentVersionAction.nameFromValue(childDefn.getOnParentVersion()).toUpperCase();
        boolean sameNameSiblings = booleanFrom(childDefn.allowsSameNameSiblings(), false);

        // Create the node for the node type ...
        if (name == null) name = JcrNodeType.RESIDUAL_NAME;
        Path path = pathFactory.create(parentPath, JcrLexicon.CHILD_NODE_DEFINITION);

        PropertyFactory factory = childDefn.getExecutionContext().getPropertyFactory();
        destination.create(path,
                           factory.create(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.CHILD_NODE_DEFINITION),
                           factory.create(JcrLexicon.REQUIRED_PRIMARY_TYPES, (Object[])requiredPrimaryTypes),
                           factory.create(JcrLexicon.DEFAULT_PRIMARY_TYPE, defaultPrimaryType),
                           factory.create(JcrLexicon.MANDATORY, mandatory),
                           factory.create(JcrLexicon.NAME, name),
                           factory.create(JcrLexicon.AUTO_CREATED, autoCreated),
                           factory.create(JcrLexicon.PROTECTED, isProtected),
                           factory.create(JcrLexicon.ON_PARENT_VERSION, onParentVersion),
                           factory.create(JcrLexicon.SAME_NAME_SIBLINGS, sameNameSiblings));

        return path;
    }
}
