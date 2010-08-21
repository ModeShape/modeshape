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
package org.modeshape.sequencer.teiid;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Property;

/**
 * 
 */
public class XmiGraphReaderTest {

    private Graph graph;
    private ExecutionContext context;
    private XmiGraphReader reader;
    private Subgraph subgraph;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("Source");
        graph = Graph.create(source, context);
        subgraph = graph.getSubgraphOfDepth(1).at("/");
        reader = new XmiGraphReader(subgraph, true);

        NamespaceRegistry registry = context.getNamespaceRegistry();
        registry.register("ecore", "http://www.eclipse.org/emf/2002/Ecore");
        registry.register("xsd", "http://www.eclipse.org/xsd/2002/XSD");
    }

    @Test
    public void shouldConvertNamesToProperForm() {
        assertNameConversion("ecore:DataType", "ecore:dataType");
        assertNameConversion("ecore:Data_Type", "ecore:dataType");
        assertNameConversion("ecore:Data-Type", "ecore:dataType");
        assertNameConversion("ecore:Data.Type", "ecore:dataType");
    }

    @Test
    public void shouldFindNameWithUrlReference() {
        assertNameConversion("ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString", "ecore:eString");
        assertNameConversion("http://www.eclipse.org/emf/2002/Ecore#//EString", "ecore:eString");
        assertNameConversion("#//XSDProcessContents", "xsdProcessContents");
        assertNameConversion("http://www.eclipse.org/xsd/2002/XSD#//XSDProcessContents", "xsd:processContents");
    }

    @Test
    public void shouldDetermineJcrPropertyTypeForTypeReference() {
        assertTypeConversion("ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EString", "STRING");
        assertTypeConversion("http://www.eclipse.org/emf/2002/Ecore#//EString", "STRING");
        assertTypeConversion("ecore:EDataType http://www.eclipse.org/emf/2002/Ecore#//EBoolean", "BOOLEAN");
    }

    @Test
    public void shouldDiscoverNoLocalReferencesInSingleValuedPropertyWithNonUuidValues() {
        assertThat(reader.references(property("p1", "string value")), is(nullValue()));
        assertThat(reader.references(property("p1", "64a77783-684b-1edf-ad26-eaf131c5fef9")), is(nullValue()));
        assertThat(reader.references(property("p1", "mmuuid/64a77783")), is(nullValue()));
    }

    @Test
    public void shouldDiscoverLocalReferencesInSingleValuedPropertyWithOneReference() {
        Property property = property("p1", "mmuuid/64a77783-684b-1edf-ad26-eaf131c5fef9");
        List<UUID> uuids = reader.references(property);
        assertThat(uuids.size(), is(1));
        assertThat(uuids.get(0), is(UUID.fromString("64a77783-684b-1edf-ad26-eaf131c5fef9")));
    }

    @Test
    public void shouldDiscoverLocalReferencesInSingleValuedPropertyWithTwoReferences() {
        Property property = property("p1",
                                     "mmuuid/64a77783-684b-1edf-ad26-eaf131c5fef9 mmuuid/87ffedc0-684b-1edf-ad26-eaf131c5fef9");
        List<UUID> uuids = reader.references(property);
        assertThat(uuids.size(), is(2));
        assertThat(uuids.get(0), is(UUID.fromString("64a77783-684b-1edf-ad26-eaf131c5fef9")));
        assertThat(uuids.get(1), is(UUID.fromString("87ffedc0-684b-1edf-ad26-eaf131c5fef9")));
    }

    @Test
    public void shouldDiscoverLocalReferencesInSingleValuedPropertyWithThreeReferences() {
        Property property = property("p1",
                                     "mmuuid/64a77783-684b-1edf-ad26-eaf131c5fef9 mmuuid/87ffedc0-684b-1edf-ad26-eaf131c5fef9 mmuuid/7b018340-684b-1edf-ad26-eaf131c5fef9");
        List<UUID> uuids = reader.references(property);
        assertThat(uuids.size(), is(3));
        assertThat(uuids.get(0), is(UUID.fromString("64a77783-684b-1edf-ad26-eaf131c5fef9")));
        assertThat(uuids.get(1), is(UUID.fromString("87ffedc0-684b-1edf-ad26-eaf131c5fef9")));
        assertThat(uuids.get(2), is(UUID.fromString("7b018340-684b-1edf-ad26-eaf131c5fef9")));
    }

    @Test
    public void shouldDiscoverLocalReferencesInMultiValuedPropertyWithOneReferenceInEachValue() {
        Property property = property("p1",
                                     "mmuuid/64a77783-684b-1edf-ad26-eaf131c5fef9",
                                     "mmuuid/87ffedc0-684b-1edf-ad26-eaf131c5fef9");
        List<UUID> uuids = reader.references(property);
        assertThat(uuids.size(), is(2));
        assertThat(uuids.get(0), is(UUID.fromString("64a77783-684b-1edf-ad26-eaf131c5fef9")));
        assertThat(uuids.get(1), is(UUID.fromString("87ffedc0-684b-1edf-ad26-eaf131c5fef9")));
    }

    @Test
    public void shouldDiscoverLocalReferencesInMultiValuedPropertyWithVariousReferencesInEachValue() {
        Property property = property("p1",
                                     "mmuuid/64a77783-684b-1edf-ad26-eaf131c5fef9",
                                     "mmuuid/87ffedc0-684b-1edf-ad26-eaf131c5fef9 mmuuid/7b018340-684b-1edf-ad26-eaf131c5fef9");
        List<UUID> uuids = reader.references(property);
        assertThat(uuids.size(), is(3));
        assertThat(uuids.get(0), is(UUID.fromString("64a77783-684b-1edf-ad26-eaf131c5fef9")));
        assertThat(uuids.get(1), is(UUID.fromString("87ffedc0-684b-1edf-ad26-eaf131c5fef9")));
        assertThat(uuids.get(2), is(UUID.fromString("7b018340-684b-1edf-ad26-eaf131c5fef9")));
    }

    protected Property property( String name,
                                 Object... values ) {
        Name propName = context.getValueFactories().getNameFactory().create(name);
        return context.getPropertyFactory().create(propName, values);
    }

    protected Subgraph importFrom( String fileOrResourceName ) {
        graph.importXmlFrom(fileOrResourceName);
        Subgraph subgraph = graph.getSubgraphOfDepth(20).at("/ecore:EPackage");
        return subgraph;
    }

    protected void assertNameConversion( String input,
                                         String output ) {
        Name actual = reader.nameFrom(input);
        Name expected = context.getValueFactories().getNameFactory().create(output);
        assertThat(actual, is(expected));
    }

    protected void assertTypeConversion( String input,
                                         String expectedPropertyTypeName ) {
        Name actual = reader.nameFrom(input);
        String actualTypeName = reader.jcrTypeNameFor(actual);
        assertThat(actualTypeName, is(expectedPropertyTypeName));
    }
}
