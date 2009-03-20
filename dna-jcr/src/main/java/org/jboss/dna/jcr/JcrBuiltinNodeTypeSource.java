/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.basic.BasicName;

/**
 * {@link JcrNodeTypeSource} that provides built-in node types per the 1.0 specification.
 */
@Immutable
class JcrBuiltinNodeTypeSource extends AbstractJcrNodeTypeSource {

    /** The list of primary node types. */
    private final List<JcrNodeType> primaryNodeTypes;
    /** The list of mixin node types. */
    private final List<JcrNodeType> mixinNodeTypes;

    JcrBuiltinNodeTypeSource( ExecutionContext context ) {
        this(context, null);
    }

    JcrBuiltinNodeTypeSource( ExecutionContext context,
                              JcrNodeTypeSource predecessor ) {
        super(predecessor);

        primaryNodeTypes = new ArrayList<JcrNodeType>();

        /*
         * These values get created without a session cache, as they aren't tied to any particular session.
         */
        Value trueValue = new JcrValue(context.getValueFactories(), null, PropertyType.BOOLEAN, Boolean.TRUE);
        Value ntBaseValue = new JcrValue(context.getValueFactories(), null, PropertyType.NAME, JcrNtLexicon.BASE);

        // Stubbing in child node and property definitions for now
        JcrNodeType base = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, JcrNtLexicon.BASE, NO_SUPERTYPES, NO_PRIMARY_ITEM_NAME,
                                           NO_CHILD_NODES, Arrays.asList(new JcrPropertyDefinition[] {
                                               new JcrPropertyDefinition(context, null, JcrLexicon.PRIMARY_TYPE,
                                                                         OnParentVersionBehavior.COMPUTE.getJcrValue(), true,
                                                                         true, true, NO_DEFAULT_VALUES, PropertyType.NAME,
                                                                         NO_CONSTRAINTS, false),
                                               new JcrPropertyDefinition(context, null, JcrLexicon.MIXIN_TYPES,
                                                                         OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                         false, true, NO_DEFAULT_VALUES, PropertyType.NAME,
                                                                         NO_CONSTRAINTS, true)}), NOT_MIXIN,
                                           UNORDERABLE_CHILD_NODES);

        // This needs to be declared early, as some of the primary types reference it
        JcrNodeType referenceable = new JcrNodeType(
                                                    context,
                                                    NO_NODE_TYPE_MANAGER,
                                                    JcrMixLexicon.REFERENCEABLE,
                                                    NO_SUPERTYPES,
                                                    NO_PRIMARY_ITEM_NAME,
                                                    NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         JcrLexicon.UUID,
                                                                                                                         OnParentVersionBehavior.INITIALIZE.getJcrValue(),
                                                                                                                         true,
                                                                                                                         true,
                                                                                                                         true,
                                                                                                                         NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.STRING,
                                                                                                                         NO_CONSTRAINTS,
                                                                                                                         false),}),
                                                    IS_A_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType childNodeDefinition = new JcrNodeType(
                                                          context,
                                                          NO_NODE_TYPE_MANAGER,
                                                          JcrNtLexicon.CHILD_NODE_DEFINITION,
                                                          Arrays.asList(new NodeType[] {base}),
                                                          NO_PRIMARY_ITEM_NAME,
                                                          NO_CHILD_NODES,
                                                          Arrays.asList(new JcrPropertyDefinition[] {
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        JcrLexicon.AUTO_CREATED,
                                                                                        OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                        false, true, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.BOOLEAN, NO_CONSTRAINTS,
                                                                                        false),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        JcrLexicon.DEFAULT_PRIMARY_TYPE,
                                                                                        OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.NAME, NO_CONSTRAINTS, false),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        JcrLexicon.MANDATORY,
                                                                                        OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                        false, true, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.BOOLEAN, NO_CONSTRAINTS,
                                                                                        false),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        JcrLexicon.NAME,
                                                                                        OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                        false, false, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.NAME, NO_CONSTRAINTS, false),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        JcrLexicon.ON_PARENT_VERSION,
                                                                                        OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                        false, true, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.STRING, NO_CONSTRAINTS,
                                                                                        false),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        JcrLexicon.PROTECTED,
                                                                                        OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                        false, true, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.BOOLEAN, NO_CONSTRAINTS,
                                                                                        false),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        JcrLexicon.REQUIRED_PRIMARY_TYPES,
                                                                                        OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                        false, true, false,
                                                                                        new Value[] {ntBaseValue},
                                                                                        PropertyType.NAME, NO_CONSTRAINTS, true),
                                                              new JcrPropertyDefinition(
                                                                                        context,
                                                                                        null,
                                                                                        JcrLexicon.SAME_NAME_SIBLINGS,
                                                                                        OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                        false, true, false, NO_DEFAULT_VALUES,
                                                                                        PropertyType.BOOLEAN, NO_CONSTRAINTS,
                                                                                        false)}), NOT_MIXIN,
                                                          UNORDERABLE_CHILD_NODES);

        JcrNodeType hierarchyNode = new JcrNodeType(
                                                    context,
                                                    NO_NODE_TYPE_MANAGER,
                                                    JcrNtLexicon.HIERARCHY_NODE,
                                                    Arrays.asList(new NodeType[] {base}),
                                                    NO_PRIMARY_ITEM_NAME,
                                                    NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         JcrLexicon.CREATED,
                                                                                                                         OnParentVersionBehavior.INITIALIZE.getJcrValue(),
                                                                                                                         true,
                                                                                                                         false,
                                                                                                                         true,
                                                                                                                         NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.DATE,
                                                                                                                         NO_CONSTRAINTS,
                                                                                                                         false),}),
                                                    NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType file = new JcrNodeType(
                                           context,
                                           NO_NODE_TYPE_MANAGER,
                                           JcrNtLexicon.FILE,
                                           Arrays.asList(new NodeType[] {hierarchyNode}),
                                           JcrLexicon.CONTENT,
                                           Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                        context,
                                                                                                        null,
                                                                                                        JcrLexicon.CONTENT,
                                                                                                        OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                                        false, true, false,
                                                                                                        false, null,
                                                                                                        new NodeType[] {base})}),
                                           NO_PROPERTIES, NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType folder = new JcrNodeType(
                                             context,
                                             NO_NODE_TYPE_MANAGER,
                                             JcrNtLexicon.FOLDER,
                                             Arrays.asList(new NodeType[] {hierarchyNode}),
                                             NO_PRIMARY_ITEM_NAME,
                                             Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                          context,
                                                                                                          null,
                                                                                                          null,
                                                                                                          OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                          false,
                                                                                                          false,
                                                                                                          false,
                                                                                                          false,
                                                                                                          null,
                                                                                                          new NodeType[] {hierarchyNode})}),
                                             NO_PROPERTIES, NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType frozenNode = new JcrNodeType(
                                                 context,
                                                 NO_NODE_TYPE_MANAGER,
                                                 JcrNtLexicon.FROZEN_NODE,
                                                 Arrays.asList(new NodeType[] {base, referenceable}),
                                                 NO_PRIMARY_ITEM_NAME,
                                                 Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                              context,
                                                                                                              null,
                                                                                                              ALL_NODES,
                                                                                                              OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                                                              false,
                                                                                                              false,
                                                                                                              true,
                                                                                                              true,
                                                                                                              null,
                                                                                                              new NodeType[] {base})}),
                                                 Arrays.asList(new JcrPropertyDefinition[] {
                                                     new JcrPropertyDefinition(context, null, JcrLexicon.FROZEN_MIXIN_TYPES,
                                                                               OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                               false, false, true, NO_DEFAULT_VALUES,
                                                                               PropertyType.NAME, NO_CONSTRAINTS, true),
                                                     new JcrPropertyDefinition(context, null, JcrLexicon.FROZEN_PRIMARY_TYPE,
                                                                               OnParentVersionBehavior.ABORT.getJcrValue(), true,
                                                                               true, true, NO_DEFAULT_VALUES, PropertyType.NAME,
                                                                               NO_CONSTRAINTS, false),
                                                     new JcrPropertyDefinition(context, null, JcrLexicon.FROZEN_UUID,
                                                                               OnParentVersionBehavior.ABORT.getJcrValue(), true,
                                                                               true, true, NO_DEFAULT_VALUES,
                                                                               PropertyType.STRING, NO_CONSTRAINTS, false),
                                                     new JcrPropertyDefinition(context, null, ALL_NODES,
                                                                               OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                               false, false, true, NO_DEFAULT_VALUES,
                                                                               PropertyType.UNDEFINED, NO_CONSTRAINTS, false),
                                                     new JcrPropertyDefinition(context, null, ALL_NODES,
                                                                               OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                               false, false, true, NO_DEFAULT_VALUES,
                                                                               PropertyType.UNDEFINED, NO_CONSTRAINTS, true),}),
                                                 NOT_MIXIN, ORDERABLE_CHILD_NODES);

        JcrNodeType linkedFile = new JcrNodeType(
                                                 context,
                                                 NO_NODE_TYPE_MANAGER,
                                                 JcrNtLexicon.LINKED_FILE,
                                                 Arrays.asList(new NodeType[] {hierarchyNode}),
                                                 JcrLexicon.CONTENT,
                                                 NO_CHILD_NODES,
                                                 Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                      context,
                                                                                                                      null,
                                                                                                                      JcrLexicon.CONTENT,
                                                                                                                      OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                                                      false,
                                                                                                                      true,
                                                                                                                      false,
                                                                                                                      NO_DEFAULT_VALUES,
                                                                                                                      PropertyType.REFERENCE,
                                                                                                                      NO_CONSTRAINTS,
                                                                                                                      false),}),
                                                 NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        // Had to be moved above nodeType due to dependency
        JcrNodeType propertyDefinition = new JcrNodeType(
                                                         context,
                                                         NO_NODE_TYPE_MANAGER,
                                                         JcrNtLexicon.PROPERTY_DEFINITION,
                                                         Arrays.asList(new NodeType[] {base}),
                                                         NO_PRIMARY_ITEM_NAME,
                                                         NO_CHILD_NODES,
                                                         Arrays.asList(new JcrPropertyDefinition[] {
                                                             new JcrPropertyDefinition(
                                                                                       context,
                                                                                       null,
                                                                                       JcrLexicon.AUTO_CREATED,
                                                                                       OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                       false, true, false, NO_DEFAULT_VALUES,
                                                                                       PropertyType.BOOLEAN, NO_CONSTRAINTS,
                                                                                       false),
                                                             new JcrPropertyDefinition(
                                                                                       context,
                                                                                       null,
                                                                                       JcrLexicon.DEFAULT_VALUES,
                                                                                       OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                       false, false, false, NO_DEFAULT_VALUES,
                                                                                       PropertyType.UNDEFINED, NO_CONSTRAINTS,
                                                                                       true),
                                                             new JcrPropertyDefinition(
                                                                                       context,
                                                                                       null,
                                                                                       JcrLexicon.MANDATORY,
                                                                                       OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                       false, true, false, NO_DEFAULT_VALUES,
                                                                                       PropertyType.BOOLEAN, NO_CONSTRAINTS,
                                                                                       false),
                                                             new JcrPropertyDefinition(
                                                                                       context,
                                                                                       null,
                                                                                       JcrLexicon.MULTIPLE,
                                                                                       OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                       false, true, false, NO_DEFAULT_VALUES,
                                                                                       PropertyType.BOOLEAN, NO_CONSTRAINTS,
                                                                                       false),
                                                             new JcrPropertyDefinition(
                                                                                       context,
                                                                                       null,
                                                                                       JcrLexicon.NAME,
                                                                                       OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                       false, false, false, NO_DEFAULT_VALUES,
                                                                                       PropertyType.NAME, NO_CONSTRAINTS, false),
                                                             new JcrPropertyDefinition(
                                                                                       context,
                                                                                       null,
                                                                                       JcrLexicon.ON_PARENT_VERSION,
                                                                                       OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                       false, true, false, NO_DEFAULT_VALUES,
                                                                                       PropertyType.STRING, NO_CONSTRAINTS, false),
                                                             new JcrPropertyDefinition(
                                                                                       context,
                                                                                       null,
                                                                                       JcrLexicon.PROTECTED,
                                                                                       OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                       false, true, false, NO_DEFAULT_VALUES,
                                                                                       PropertyType.BOOLEAN, NO_CONSTRAINTS,
                                                                                       false),
                                                             new JcrPropertyDefinition(
                                                                                       context,
                                                                                       null,
                                                                                       JcrLexicon.REQUIRED_TYPE,
                                                                                       OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                       false, true, false, NO_DEFAULT_VALUES,
                                                                                       PropertyType.STRING, NO_CONSTRAINTS, false),
                                                             new JcrPropertyDefinition(
                                                                                       context,
                                                                                       null,
                                                                                       JcrLexicon.VALUE_CONSTRAINTS,
                                                                                       OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                       false, false, false, NO_DEFAULT_VALUES,
                                                                                       PropertyType.STRING, NO_CONSTRAINTS, true)}),
                                                         NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType nodeType = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, JcrNtLexicon.NODE_TYPE,
                                               Arrays.asList(new NodeType[] {base}), NO_PRIMARY_ITEM_NAME,
                                               Arrays.asList(new JcrNodeDefinition[] {
                                                   new JcrNodeDefinition(context, null, JcrLexicon.CHILD_NODE_DEFINITION,
                                                                         OnParentVersionBehavior.VERSION.getJcrValue(), false,
                                                                         false, false, true, JcrNtLexicon.CHILD_NODE_DEFINITION,
                                                                         new NodeType[] {childNodeDefinition}),
                                                   new JcrNodeDefinition(context, null, JcrLexicon.PROPERTY_DEFINITION,
                                                                         OnParentVersionBehavior.VERSION.getJcrValue(), false,
                                                                         false, false, true, JcrNtLexicon.PROPERTY_DEFINITION,
                                                                         new NodeType[] {propertyDefinition})}),
                                               Arrays.asList(new JcrPropertyDefinition[] {
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.HAS_ORDERABLE_CHILD_NODES,
                                                                             OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                             true, false, NO_DEFAULT_VALUES,
                                                                             PropertyType.BOOLEAN, NO_CONSTRAINTS, false),
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.IS_MIXIN,
                                                                             OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                             true, false, NO_DEFAULT_VALUES,
                                                                             PropertyType.BOOLEAN, NO_CONSTRAINTS, false),
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.NODE_TYPE_NAME,
                                                                             OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                             true, false, NO_DEFAULT_VALUES, PropertyType.NAME,
                                                                             NO_CONSTRAINTS, false),
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.PRIMARY_ITEM_NAME,
                                                                             OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                             false, false, NO_DEFAULT_VALUES, PropertyType.NAME,
                                                                             NO_CONSTRAINTS, false),
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.SUPERTYPES,
                                                                             OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                             false, false, NO_DEFAULT_VALUES, PropertyType.NAME,
                                                                             NO_CONSTRAINTS, true),}), NOT_MIXIN,
                                               UNORDERABLE_CHILD_NODES);

        JcrNodeType query = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, JcrNtLexicon.QUERY,
                                            Arrays.asList(new NodeType[] {base}), NO_PRIMARY_ITEM_NAME, NO_CHILD_NODES,
                                            Arrays.asList(new JcrPropertyDefinition[] {
                                                new JcrPropertyDefinition(context, null, JcrLexicon.LANGUAGE,
                                                                          OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                          false, false, NO_DEFAULT_VALUES, PropertyType.STRING,
                                                                          NO_CONSTRAINTS, false),
                                                new JcrPropertyDefinition(context, null, JcrLexicon.STATEMENT,
                                                                          OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                          false, false, NO_DEFAULT_VALUES, PropertyType.STRING,
                                                                          NO_CONSTRAINTS, false),}), NOT_MIXIN,
                                            UNORDERABLE_CHILD_NODES);

        JcrNodeType resource = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, JcrNtLexicon.RESOURCE,
                                               Arrays.asList(new NodeType[] {base, referenceable}), JcrLexicon.DATA,
                                               NO_CHILD_NODES, Arrays.asList(new JcrPropertyDefinition[] {
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.DATA,
                                                                             OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                             true, false, NO_DEFAULT_VALUES, PropertyType.BINARY,
                                                                             NO_CONSTRAINTS, false),
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.ENCODING,
                                                                             OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                             false, false, NO_DEFAULT_VALUES,
                                                                             PropertyType.STRING, NO_CONSTRAINTS, false),
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.LAST_MODIFIED,
                                                                             OnParentVersionBehavior.IGNORE.getJcrValue(), false,
                                                                             true, false, NO_DEFAULT_VALUES, PropertyType.DATE,
                                                                             NO_CONSTRAINTS, false),
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.MIMETYPE,
                                                                             OnParentVersionBehavior.COPY.getJcrValue(), false,
                                                                             true, false, NO_DEFAULT_VALUES, PropertyType.STRING,
                                                                             NO_CONSTRAINTS, false),}), NOT_MIXIN,
                                               UNORDERABLE_CHILD_NODES);

        JcrNodeType unstructured = new JcrNodeType(
                                                   context,
                                                   NO_NODE_TYPE_MANAGER,
                                                   JcrNtLexicon.UNSTRUCTURED,
                                                   Arrays.asList(new NodeType[] {base}),
                                                   NO_PRIMARY_ITEM_NAME,
                                                   Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                                context,
                                                                                                                null,
                                                                                                                ALL_NODES,
                                                                                                                OnParentVersionBehavior.VERSION.getJcrValue(),
                                                                                                                false,
                                                                                                                false,
                                                                                                                false,
                                                                                                                true,
                                                                                                                JcrNtLexicon.UNSTRUCTURED,
                                                                                                                new NodeType[] {base}),}),
                                                   Arrays.asList(new JcrPropertyDefinition[] {
                                                       new JcrPropertyDefinition(context, null, ALL_NODES,
                                                                                 OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                 false, false, false, NO_DEFAULT_VALUES,
                                                                                 PropertyType.UNDEFINED, NO_CONSTRAINTS, false),
                                                       new JcrPropertyDefinition(context, null, ALL_NODES,
                                                                                 OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                 false, false, false, NO_DEFAULT_VALUES,
                                                                                 PropertyType.UNDEFINED, NO_CONSTRAINTS, true),}),
                                                   NOT_MIXIN, ORDERABLE_CHILD_NODES);

        JcrNodeType version = new JcrNodeType(
                                              context,
                                              NO_NODE_TYPE_MANAGER,
                                              JcrNtLexicon.VERSION,
                                              Arrays.asList(new NodeType[] {base, referenceable}),
                                              NO_PRIMARY_ITEM_NAME,
                                              Arrays.asList(new JcrNodeDefinition[] {new JcrNodeDefinition(
                                                                                                           context,
                                                                                                           null,
                                                                                                           JcrLexicon.FROZEN_NODE,
                                                                                                           OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                                                           false,
                                                                                                           false,
                                                                                                           true,
                                                                                                           false,
                                                                                                           null,
                                                                                                           new NodeType[] {frozenNode}),}),
                                              Arrays.asList(new JcrPropertyDefinition[] {
                                                  new JcrPropertyDefinition(context, null, JcrLexicon.CREATED,
                                                                            OnParentVersionBehavior.ABORT.getJcrValue(), true,
                                                                            true, true, NO_DEFAULT_VALUES, PropertyType.DATE,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, JcrLexicon.PREDECESSORS,
                                                                            OnParentVersionBehavior.ABORT.getJcrValue(), false,
                                                                            false, true, NO_DEFAULT_VALUES,
                                                                            PropertyType.REFERENCE, NO_CONSTRAINTS, true),
                                                  new JcrPropertyDefinition(context, null, JcrLexicon.SUCCESSORS,
                                                                            OnParentVersionBehavior.ABORT.getJcrValue(), false,
                                                                            false, true, NO_DEFAULT_VALUES,
                                                                            PropertyType.REFERENCE, NO_CONSTRAINTS, true),}),
                                              NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType versionLabels = new JcrNodeType(
                                                    context,
                                                    NO_NODE_TYPE_MANAGER,
                                                    JcrNtLexicon.VERSION_LABELS,
                                                    Arrays.asList(new NodeType[] {base}),
                                                    NO_PRIMARY_ITEM_NAME,
                                                    NO_CHILD_NODES,
                                                    Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                         context,
                                                                                                                         null,
                                                                                                                         ALL_NODES,
                                                                                                                         OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                                                                         false,
                                                                                                                         false,
                                                                                                                         true,
                                                                                                                         NO_DEFAULT_VALUES,
                                                                                                                         PropertyType.REFERENCE,
                                                                                                                         NO_CONSTRAINTS,
                                                                                                                         false),}),
                                                    NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        JcrNodeType versionHistory = new JcrNodeType(
                                                     context,
                                                     NO_NODE_TYPE_MANAGER,
                                                     JcrNtLexicon.VERSION_HISTORY,
                                                     Arrays.asList(new NodeType[] {base, referenceable}),
                                                     NO_PRIMARY_ITEM_NAME,
                                                     Arrays.asList(new JcrNodeDefinition[] {
                                                         new JcrNodeDefinition(context, null, JcrLexicon.ROOT_VERSION,
                                                                               OnParentVersionBehavior.ABORT.getJcrValue(), true,
                                                                               true, true, false, JcrNtLexicon.VERSION,
                                                                               new NodeType[] {version}),
                                                         new JcrNodeDefinition(context, null, JcrLexicon.VERSION_LABELS,
                                                                               OnParentVersionBehavior.ABORT.getJcrValue(), true,
                                                                               true, true, false, JcrNtLexicon.VERSION_LABELS,
                                                                               new NodeType[] {versionLabels}),
                                                         new JcrNodeDefinition(context, null, ALL_NODES,
                                                                               OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                               false, false, true, false, JcrNtLexicon.VERSION,
                                                                               new NodeType[] {version}),}),
                                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                          context,
                                                                                                                          null,
                                                                                                                          JcrLexicon.VERSIONABLE_UUID,
                                                                                                                          OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                                                                          true,
                                                                                                                          true,
                                                                                                                          true,
                                                                                                                          NO_DEFAULT_VALUES,
                                                                                                                          PropertyType.STRING,
                                                                                                                          NO_CONSTRAINTS,
                                                                                                                          false),}),
                                                     NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        Name CHILD_VERSION_HISTORY = new BasicName(JcrLexicon.Namespace.URI, "childVersionHistory");
        JcrNodeType versionedChild = new JcrNodeType(
                                                     context,
                                                     NO_NODE_TYPE_MANAGER,
                                                     JcrNtLexicon.VERSIONED_CHILD,
                                                     Arrays.asList(new NodeType[] {base}),
                                                     NO_PRIMARY_ITEM_NAME,
                                                     NO_CHILD_NODES,
                                                     Arrays.asList(new JcrPropertyDefinition[] {new JcrPropertyDefinition(
                                                                                                                          context,
                                                                                                                          null,
                                                                                                                          CHILD_VERSION_HISTORY,
                                                                                                                          OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                                                                          true,
                                                                                                                          true,
                                                                                                                          true,
                                                                                                                          NO_DEFAULT_VALUES,
                                                                                                                          PropertyType.REFERENCE,
                                                                                                                          NO_CONSTRAINTS,
                                                                                                                          false),}),
                                                     NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        primaryNodeTypes.addAll(Arrays.asList(new JcrNodeType[] {base, unstructured, childNodeDefinition, file, folder,
            frozenNode, hierarchyNode, linkedFile, nodeType, propertyDefinition, query, resource, nodeType, version,
            versionHistory, versionLabels, versionedChild}));

        mixinNodeTypes = new ArrayList<JcrNodeType>();

        JcrNodeType lockable = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, JcrMixLexicon.LOCKABLE, NO_SUPERTYPES,
                                               NO_PRIMARY_ITEM_NAME, NO_CHILD_NODES, Arrays.asList(new JcrPropertyDefinition[] {
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.LOCK_IS_DEEP,
                                                                             OnParentVersionBehavior.IGNORE.getJcrValue(), false,
                                                                             false, true, NO_DEFAULT_VALUES,
                                                                             PropertyType.BOOLEAN, NO_CONSTRAINTS, false),
                                                   new JcrPropertyDefinition(context, null, JcrLexicon.LOCK_OWNER,
                                                                             OnParentVersionBehavior.IGNORE.getJcrValue(), false,
                                                                             false, true, NO_DEFAULT_VALUES, PropertyType.STRING,
                                                                             NO_CONSTRAINTS, false)}), IS_A_MIXIN,
                                               UNORDERABLE_CHILD_NODES);

        JcrNodeType versionable = new JcrNodeType(
                                                  context,
                                                  NO_NODE_TYPE_MANAGER,
                                                  JcrMixLexicon.VERSIONABLE,
                                                  Arrays.asList(new NodeType[] {referenceable}),
                                                  NO_PRIMARY_ITEM_NAME,
                                                  NO_CHILD_NODES,
                                                  Arrays.asList(new JcrPropertyDefinition[] {
                                                      new JcrPropertyDefinition(context, null, JcrLexicon.BASE_VERSION,
                                                                                OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                false, true, true, NO_DEFAULT_VALUES,
                                                                                PropertyType.REFERENCE, NO_CONSTRAINTS, false),
                                                      new JcrPropertyDefinition(context, null, JcrLexicon.IS_CHECKED_OUT,
                                                                                OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                true, true, true, new Value[] {trueValue},
                                                                                PropertyType.BOOLEAN, NO_CONSTRAINTS, false),
                                                      new JcrPropertyDefinition(context, null, JcrLexicon.MERGE_FAILED,
                                                                                OnParentVersionBehavior.ABORT.getJcrValue(),
                                                                                false, false, true, NO_DEFAULT_VALUES,
                                                                                PropertyType.REFERENCE, NO_CONSTRAINTS, true),
                                                      new JcrPropertyDefinition(context, null, JcrLexicon.PREDECESSORS,
                                                                                OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                false, true, true, NO_DEFAULT_VALUES,
                                                                                PropertyType.REFERENCE, NO_CONSTRAINTS, true),
                                                      new JcrPropertyDefinition(context, null, JcrLexicon.VERSION_HISTORY,
                                                                                OnParentVersionBehavior.COPY.getJcrValue(),
                                                                                false, true, true, NO_DEFAULT_VALUES,
                                                                                PropertyType.REFERENCE, NO_CONSTRAINTS, false),}),
                                                  IS_A_MIXIN, UNORDERABLE_CHILD_NODES);

        mixinNodeTypes.addAll(Arrays.asList(new JcrNodeType[] {lockable, referenceable, versionable}));

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.JcrNodeTypeSource#getMixinNodeTypes()
     */
    @Override
    public Collection<JcrNodeType> getDeclaredMixinNodeTypes() {
        return mixinNodeTypes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.JcrNodeTypeSource#getPrimaryNodeTypes()
     */
    @Override
    public Collection<JcrNodeType> getDeclaredPrimaryNodeTypes() {
        return primaryNodeTypes;
    }

}
