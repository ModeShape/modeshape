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
package org.modeshape.jcr;

import java.util.Arrays;
import java.util.List;
import javax.jcr.PropertyType;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.version.OnParentVersionAction;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

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

    @SuppressWarnings( "unchecked" )
    public static List<NodeTypeDefinition> getNodeTypes( ExecutionContext context ) throws ConstraintViolationException {
        JcrPropertyDefinitionTemplate property;

        NodeTypeTemplate car = new JcrNodeTypeTemplate(context);
        car.setName("vehix:car");
        car.setOrderableChildNodes(true);
        car.setPrimaryItemName("vehix:model");

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:maker");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.STRING);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:model");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.STRING);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:introduced");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.LONG);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:year");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setMandatory(true);
        property.setRequiredType(PropertyType.LONG);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:msrp");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.STRING);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:userRating");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.DOUBLE);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:valueRating");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.DOUBLE);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:mpgCity");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.LONG);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:mpgHighway");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.LONG);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:lengthInInches");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.DOUBLE);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:wheelbaseInInches");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.DOUBLE);
        car.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:engine");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.STRING);
        car.getPropertyDefinitionTemplates().add(property);

        NodeTypeTemplate aircraft = new JcrNodeTypeTemplate(context);
        aircraft.setName("vehix:aircraft");
        aircraft.setDeclaredSuperTypeNames(new String[] {"nt:unstructured"});
        aircraft.setOrderableChildNodes(true);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:maker");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.STRING);
        aircraft.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:model");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.STRING);
        aircraft.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:introduced");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.LONG);
        aircraft.getPropertyDefinitionTemplates().add(property);

        property = new JcrPropertyDefinitionTemplate(context);
        property.setName("vehix:year");
        property.setOnParentVersion(OnParentVersionAction.COMPUTE);
        property.setRequiredType(PropertyType.LONG);
        aircraft.getPropertyDefinitionTemplates().add(property);

        return Arrays.asList(new NodeTypeDefinition[] {car, aircraft,});
    }
}
