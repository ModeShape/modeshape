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
package org.modeshape.graph;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.basic.BasicSingleValueProperty;
import org.modeshape.graph.property.basic.NameValueFactory;
import org.modeshape.graph.property.basic.PathValueFactory;
import org.modeshape.graph.property.basic.SimpleNamespaceRegistry;
import org.modeshape.graph.property.basic.StringValueFactory;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests <code>Location</code> class and factory constructors.
 */
public class LocationTest {

    private static final NoOpEncoder NO_OP_ENCODER = new NoOpEncoder();
    private static final NamespaceRegistry NAMESPACE_REGISTRY = new SimpleNamespaceRegistry("http://www.modeshape.org/1.0");
    private static final StringValueFactory STRING_VALUE_FACTORY = new StringValueFactory(NAMESPACE_REGISTRY, NO_OP_ENCODER,
                                                                                          NO_OP_ENCODER);
    private static final NameValueFactory NAME_VALUE_FACTORY = new NameValueFactory(NAMESPACE_REGISTRY, NO_OP_ENCODER,
                                                                                    STRING_VALUE_FACTORY);

    private PathFactory pathFactory = new PathValueFactory(NO_OP_ENCODER, STRING_VALUE_FACTORY, NAME_VALUE_FACTORY);

    private static UUID uuid = UUID.randomUUID();

    private Path pathA = pathFactory.create("/A");
    private Path pathABC = pathFactory.create("/A/B/C");

    private static Property propA = new BasicSingleValueProperty(NAME_VALUE_FACTORY.create("A"), "Value A");
    private static Property propB = new BasicSingleValueProperty(NAME_VALUE_FACTORY.create("B"), "Value B");
    private static Property propU = new BasicSingleValueProperty(ModeShapeLexicon.UUID, uuid);

    private static List<Property> propListAB;
    private static List<Property> propListABU;

    @BeforeClass
    public static void beforeAny() {
        propListAB = new ArrayList<Property>();
        propListAB.add(propA);
        propListAB.add(propB);

        propListABU = new ArrayList<Property>();
        propListABU.add(propA);
        propListABU.add(propB);
        propListABU.add(propU);

    }

    @Test
    public void locationsWithSamePathsAreEqual() {
        Location locationA1 = Location.create(pathA);
        Location locationA2 = Location.create(pathA);

        assertThat("Locations created with identical paths must be equal", locationA1, is(locationA2));

        Location locationABC1 = Location.create(pathABC);
        Location locationABC2 = Location.create(pathABC);

        assertThat("Locations created with identical non-trivial paths must be equal", locationABC1, is(locationABC2));
    }

    @Test
    public void locationsWithSamePathsAreSame() {
        Location locationA1 = Location.create(pathA);
        Location locationA2 = Location.create(pathA);

        assertThat("isSame must return true for locations created with identical paths", locationA1.isSame(locationA2), is(true));

        Location locationABC1 = Location.create(pathABC);
        Location locationABC2 = Location.create(pathABC);

        assertThat("isSame must return true for locations created with identical, non-trivial paths",
                   locationABC1.isSame(locationABC2),
                   is(true));
    }

    @Test
    public void locationsWithSamePathsAndSamePropertyAreNotEqual() {
        Location locationA1 = Location.create(pathA, propA);
        Location locationA2 = Location.create(pathA, propA);

        assertThat("Locations created with identical paths and different property must be equal", locationA1, is(locationA2));
    }

    @Test
    public void locationsWithSamePathsAndSamePropertyAreNotSame() {
        Location locationA1 = Location.create(pathA, propA);
        Location locationA2 = Location.create(pathA, propA);

        assertThat("isSame must return true for locations created with identical paths and property",
                   locationA1.isSame(locationA2),
                   is(true));
    }

    @Test
    public void locationsWithSamePathsAndDifferentPropertyAreEqual() {
        Location locationA1 = Location.create(pathA, propA);
        Location locationA2 = Location.create(pathA, propB);

        assertThat("Locations created with identical paths and different property must not be equal", locationA1.equals(locationA2), is(true));
    }

    @Test
    public void locationsWithSamePathsAndDifferentPropertyAreNotSame() {
        Location locationA1 = Location.create(pathA, propA);
        Location locationA2 = Location.create(pathA, propB);

        assertThat("isSame must not return true for locations created with identical paths and property",
                   locationA1.isSame(locationA2),
                   is(false));
    }

    @Test
    public void locationsWithDifferentPathsAndSamePropertyAreNotEqual() {
        Location locationA1 = Location.create(pathA, propA);
        Location locationA2 = Location.create(pathABC, propA);

        assertThat("Locations created with different paths and the same property must not be equal", locationA1, not(locationA2));
    }

    @Test
    public void locationsWithDifferentPathsAndSamePropertyAreNotSame() {
        Location locationA1 = Location.create(pathA, propA);
        Location locationA2 = Location.create(pathABC, propA);

        assertThat("isSame must not return true for locations created with different paths and the same property",
                   locationA1.isSame(locationA2),
                   is(false));
    }

    @Test
    public void locationsWithSamePathsAndSamePropertiesAreNotEqual() {
        Location locationA1 = Location.create(pathA, propListAB);
        Location locationA2 = Location.create(pathA, propListAB);

        assertThat("Locations created with identical paths and different properties must be equal", locationA1, is(locationA2));
    }

    @Test
    public void locationsWithSamePathsAndSamePropertiesAreNotSame() {
        Location locationA1 = Location.create(pathA, propListAB);
        Location locationA2 = Location.create(pathA, propListAB);

        assertThat("isSame must return true for locations created with identical paths and properties",
                   locationA1.isSame(locationA2),
                   is(true));
    }

    @Test
    public void locationsWithSamePathsAndDifferentPropertiesAreNotEqual() {
        Location locationA1 = Location.create(pathA, propListAB);
        Location locationA2 = Location.create(pathA, propListABU);

        assertThat("Locations created with identical paths and different properties must not be equal",
                   locationA1.equals(locationA2),
                   is(true));
    }

    @Test
    public void locationsWithSamePathsAndDifferentPropertiesAreNotSame() {
        Location locationA1 = Location.create(pathA, propListAB);
        Location locationA2 = Location.create(pathA, propListABU);

        assertThat("isSame must not return true for locations created with identical paths and different properties",
                   locationA1.isSame(locationA2),
                   is(false));
    }

    @Test
    public void locationsWithDifferentPathsAndSamePropertiesAreNotEqual() {
        Location locationA1 = Location.create(pathA, propListAB);
        Location locationA2 = Location.create(pathABC, propListAB);

        assertThat("Locations created with identical paths and different properties must not be equal",
                   locationA1,
                   not(locationA2));
    }

    @Test
    public void locationsWithDifferentPathsAndSamePropertiesAreNotSame() {
        Location locationA1 = Location.create(pathA, propListAB);
        Location locationA2 = Location.create(pathABC, propListAB);

        assertThat("isSame must not return true for locations created with different paths and the same properties",
                   locationA1.isSame(locationA2),
                   is(false));
    }

    @Test
    public void testTransitivityOfWithOperationForPathAndUUID() {
        Location locationA1 = Location.create(pathA);
        locationA1 = locationA1.with(uuid);

        Location locationU1 = Location.create(uuid);
        locationU1 = locationU1.with(pathA);

        assertThat("With() operation must be transitive for equals", locationA1.equals(locationU1), is(true));
        assertThat("With() operation must be transitive for isSame", locationA1.isSame(locationU1), is(true));
        assertThat("With() operation must be transitive for getString",
                   locationA1.getString().equals(locationU1.getString()),
                   is(true));
        assertThat("With() operation must be transitive for hashCode", locationA1.hashCode() == locationU1.hashCode(), is(true));
    }

    @Test
    public void testTransitivityOfWithOperationForPathAndProperty() {
        Location locationA1 = Location.create(pathA);
        locationA1 = locationA1.with(propB);

        Location locationU1 = Location.create(propB);
        locationU1 = locationU1.with(pathA);

        assertThat("With() operation must be transitive for equals", locationA1.equals(locationU1), is(true));
        assertThat("With() operation must be transitive for isSame", locationA1.isSame(locationU1), is(true));
        assertThat("With() operation must be transitive for getString",
                   locationA1.getString().equals(locationU1.getString()),
                   is(true));
        assertThat("With() operation must be transitive for hashCode", locationA1.hashCode() == locationU1.hashCode(), is(true));
    }

}
