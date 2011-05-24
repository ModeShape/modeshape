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
package org.modeshape.sequencer.wsdl;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.InputStream;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.sramp.SrampLexicon;
import org.modeshape.sequencer.xsd.XsdLexicon;

/**
 * 
 */
public class Wsdl11ReaderTest {

    private ExecutionContext execContext;
    private MockSequencerOutput output;
    private StreamSequencerContext context;
    private Wsdl11Reader reader;
    private boolean print;

    @Before
    public void beforeEach() {
        print = false;
        execContext = new ExecutionContext();
        execContext.getNamespaceRegistry().register(XsdLexicon.Namespace.PREFIX, XsdLexicon.Namespace.URI);
        execContext.getNamespaceRegistry().register(WsdlLexicon.Namespace.PREFIX, WsdlLexicon.Namespace.URI);
        execContext.getNamespaceRegistry().register(SrampLexicon.Namespace.PREFIX, SrampLexicon.Namespace.URI);
    }

    @After
    public void afterEach() {
        System.out.flush();
        System.err.flush();
        context = null;
        output = null;
        reader = null;
    }

    protected void read( String pathToFile ) {
        InputStream stream = load(pathToFile);
        context = new MockSequencerContext(execContext, pathToFile);
        output = new MockSequencerOutput(context, true);
        reader = new Wsdl11Reader(output, context);
        reader.read(stream, context.getInputPath());
        if (context.getProblems().hasProblems()) {
            System.out.println(context.getProblems());
            fail("At least one problem sequencing \"" + pathToFile + "\"");
        }
    }

    @Test
    public void shouldBeAbleToParse_loanServicePT() {
        read("loanServicePT.wsdl");
        print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_api_v3_portType() {
        read("uddi_api_v3_portType.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_custody_v3_binding() {
        read("uddi_custody_v3_binding.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_repl_v3_binding() {
        read("uddi_repl_v3_binding.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_repl_v3_portType() {
        read("uddi_repl_v3_portType.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_sub_v3_binding() {
        read("uddi_sub_v3_binding.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_sub_v3_portType() {
        read("uddi_sub_v3_portType.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_sbr_v3_binding() {
        read("uddi_subr_v3_binding.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_v3_service() {
        read("uddi_v3_service.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_vs_v3_binding() {
        read("uddi_vs_v3_binding.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_vs_v3_portType() {
        read("uddi_vs_v3_portType.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_vscache_v3_binding() {
        read("uddi_vscache_v3_binding.wsdl");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParse_uddi_vscache_v3_portType() {
        read("uddi_vscache_v3_portType.wsdl");
        // print = true;
        printOutput();
    }

    protected void printOutput() {
        if (print) {
            for (Path path : output.getOrderOfCreation()) {
                Map<Name, Property> props = output.getProperties(path);
                for (Map.Entry<Name, Property> entry : props.entrySet()) {
                    System.out.println(string(path) + " " + entry.getValue().getString(context.getNamespaceRegistry()));
                }
            }
        }
    }

    protected InputStream load( String resourcePath ) {
        InputStream stream = this.getClass().getResourceAsStream("/" + resourcePath);
        assertThat(stream, is(notNullValue()));
        return stream;
    }

    protected final Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected final Path relativePath( Name... segments ) {
        return context.getValueFactories().getPathFactory().createRelativePath(segments);
    }

    protected final String string( String path ) {
        return context.getValueFactories().getStringFactory().create(path);
    }

    protected final String string( Object value ) {
        return context.getValueFactories().getStringFactory().create(value);
    }

    protected final Name name( String path ) {
        return context.getValueFactories().getNameFactory().create(path);
    }

}
