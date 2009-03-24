/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.basic.BasicName;

/**
 * Define the node types for the "vehix" namespace.
 */
public class Vehicles {

    public static class Lexicon {
        public static class Namespace {
            public static final String URI = "http://example.com/vehicles";
            public static final String PREFIX = "vehix";
        }

        public static final Name CAR = new BasicName(Namespace.URI, "car");
        public static final Name AIRCRAFT = new BasicName(Namespace.URI, "aircraft");

        public static final Name MAKER = new BasicName(Namespace.URI, "maker");
        public static final Name MODEL = new BasicName(Namespace.URI, "model");
        public static final Name INTRODUCED = new BasicName(Namespace.URI, "introduced");
        public static final Name YEAR = new BasicName(Namespace.URI, "year");
        public static final Name MSRP = new BasicName(Namespace.URI, "msrp");
        public static final Name USER_RATING = new BasicName(Namespace.URI, "userRating");
        public static final Name VALUE_RATING = new BasicName(Namespace.URI, "valueRating");
        public static final Name MPG_CITY = new BasicName(Namespace.URI, "mpgCity");
        public static final Name MPG_HIGHWAY = new BasicName(Namespace.URI, "mpgHighway");
        public static final Name LENGTH_IN_INCHES = new BasicName(Namespace.URI, "lengthInInches");
        public static final Name WHEELBASE_IN_INCHES = new BasicName(Namespace.URI, "wheelbaseInInches");
        public static final Name ENGINE = new BasicName(Namespace.URI, "engine");

        public static final Name EMPTY_WEIGHT = new BasicName(Namespace.URI, "emptyWeight");

    }

    public static class NodeTypeSource extends AbstractJcrNodeTypeSource {

        private final List<JcrNodeType> nodeTypes;

        public NodeTypeSource( ExecutionContext context,
                               JcrNodeTypeSource predecessor ) {
            super(predecessor);
            this.nodeTypes = new ArrayList<JcrNodeType>();

            JcrNodeType base = findType(JcrNtLexicon.BASE);
            JcrNodeType unstructured = findType(JcrNtLexicon.UNSTRUCTURED);

            // Add in the "vehix:car" node type (which extends "nt:unstructured") ...
            JcrNodeType car = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, Lexicon.CAR,
                                              Arrays.asList(new JcrNodeType[] {base}), NO_PRIMARY_ITEM_NAME, NO_CHILD_NODES,
                                              Arrays.asList(new JcrPropertyDefinition[] {
                                                  new JcrPropertyDefinition(context, null, Lexicon.MAKER,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.STRING,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.MODEL,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.STRING,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.INTRODUCED,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.LONG,
                                                                            NO_CONSTRAINTS, false),
                                                  /* Year IS mandatory for car */
                                                  new JcrPropertyDefinition(context, null, Lexicon.YEAR,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            true, false, NO_DEFAULT_VALUES, PropertyType.LONG,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.MSRP,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.STRING,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.USER_RATING,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.DOUBLE,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.VALUE_RATING,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.DOUBLE,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.MPG_CITY,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.LONG,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.MPG_HIGHWAY,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.LONG,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.LENGTH_IN_INCHES,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.DOUBLE,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.WHEELBASE_IN_INCHES,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.DOUBLE,
                                                                            NO_CONSTRAINTS, false),
                                                  new JcrPropertyDefinition(context, null, Lexicon.ENGINE,
                                                                            OnParentVersionBehavior.COMPUTE.getJcrValue(), false,
                                                                            false, false, NO_DEFAULT_VALUES, PropertyType.STRING,
                                                                            NO_CONSTRAINTS, false),}), NOT_MIXIN,
                                              ORDERABLE_CHILD_NODES);

            // Add in the "vehix:aircraft" node type (which extends "nt:unstructured") ...
            JcrNodeType aircraft = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, Lexicon.AIRCRAFT,
                                                   Arrays.asList(new JcrNodeType[] {unstructured}), NO_PRIMARY_ITEM_NAME,
                                                   NO_CHILD_NODES, Arrays.asList(new JcrPropertyDefinition[] {
                                                       new JcrPropertyDefinition(context, null, Lexicon.MAKER,
                                                                                 OnParentVersionBehavior.COMPUTE.getJcrValue(),
                                                                                 false, false, false, NO_DEFAULT_VALUES,
                                                                                 PropertyType.STRING, NO_CONSTRAINTS, false),
                                                       new JcrPropertyDefinition(context, null, Lexicon.MODEL,
                                                                                 OnParentVersionBehavior.COMPUTE.getJcrValue(),
                                                                                 false, false, false, NO_DEFAULT_VALUES,
                                                                                 PropertyType.STRING, NO_CONSTRAINTS, false),
                                                       /* Year is NOT mandatory for aircraft */
                                                       new JcrPropertyDefinition(context, null, Lexicon.YEAR,
                                                                                 OnParentVersionBehavior.COMPUTE.getJcrValue(),
                                                                                 false, false, false, NO_DEFAULT_VALUES,
                                                                                 PropertyType.LONG, NO_CONSTRAINTS, false),
                                                       new JcrPropertyDefinition(context, null, Lexicon.INTRODUCED,
                                                                                 OnParentVersionBehavior.COMPUTE.getJcrValue(),
                                                                                 false, false, false, NO_DEFAULT_VALUES,
                                                                                 PropertyType.LONG, NO_CONSTRAINTS, false),}),
                                                   NOT_MIXIN, ORDERABLE_CHILD_NODES);

            nodeTypes.addAll(Arrays.asList(new JcrNodeType[] {car, aircraft,}));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.AbstractJcrNodeTypeSource#getDeclaredNodeTypes()
         */
        @Override
        public Collection<JcrNodeType> getDeclaredNodeTypes() {
            return nodeTypes;
        }

    }
}
