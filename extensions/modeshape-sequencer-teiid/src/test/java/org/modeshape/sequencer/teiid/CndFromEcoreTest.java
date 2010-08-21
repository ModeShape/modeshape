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
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.sequencer.teiid.CndFromEcore.GraphReader;

public class CndFromEcoreTest {

    private Graph graph;
    private ExecutionContext context;
    private GraphReader reader;
    private Subgraph subgraph;
    private CndFromEcore converter;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        InMemoryRepositorySource source = new InMemoryRepositorySource();
        source.setName("Source");
        graph = Graph.create(source, context);
        subgraph = graph.getSubgraphOfDepth(1).at("/");
        reader = new GraphReader(subgraph, true, true);

        NamespaceRegistry registry = context.getNamespaceRegistry();
        registry.register("ecore", "http://www.eclipse.org/emf/2002/Ecore");
        registry.register("xsd", "http://www.eclipse.org/xsd/2002/XSD");

        converter = new CndFromEcore();
    }

    @Test
    public void shouldPrintUsageForNoInputFiles() throws Exception {
        CndFromEcore.main(new String[] {"-o", "my.cnd"});
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
    public void shouldConvertRelationalEcore() {
        converter.setEcoreFileNames("src/test/resources/ecore/relational.ecore");
        converter.execute();
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
