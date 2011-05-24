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

package org.modeshape.sequencer.xsd;

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

public class XsdSequencerTest {

    private ExecutionContext execContext;
    private MockSequencerOutput output;
    private StreamSequencerContext context;
    private static XsdSequencer sequencer;
    private boolean print;

    @Before
    public void beforeEach() {
        print = false;
        execContext = new ExecutionContext();
        execContext.getNamespaceRegistry().register(XsdLexicon.Namespace.PREFIX, XsdLexicon.Namespace.URI);
        execContext.getNamespaceRegistry().register(SrampLexicon.Namespace.PREFIX, SrampLexicon.Namespace.URI);
        if (sequencer == null) sequencer = new XsdSequencer();
    }

    @After
    public void afterEach() {
        System.out.flush();
        System.err.flush();
        context = null;
        output = null;
        // sequencer = null;
    }

    protected void sequence( String pathToFile ) {
        InputStream stream = load(pathToFile);
        context = new MockSequencerContext(execContext, pathToFile);
        output = new MockSequencerOutput(context, true);
        sequencer.sequence(stream, output, context);
        if (context.getProblems().hasProblems()) {
            System.out.println(context.getProblems());
            fail("At least one problem sequencing \"" + pathToFile + "\"");
        }
    }

    @Test
    public void shouldBeAbleToParseXsdForStockQuote() {
        sequence("stockQuote.xsd");
        print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdForUddiV3() {
        sequence("uddi_v3.xsd");
        // print = true;
        printOutput();

        // Name folderName = name("test subfolder");
        // Name fileName = name("test2.txt");
        //
        // Path nodePath = relativePath(XsdLexicon.CONTENT, folderName, fileName, JcrLexicon.CONTENT);
        //
        // Property property = seqtest.getProperty(nodePath, JcrLexicon.DATA);
        // assertThat(property, is(notNullValue()));
        // assertThat(stringFactory.create(property.getFirstValue()), is("This is a test content of file2\n"));
    }

    @Test
    public void shouldBeAbleToParseXsdForUddiV3ASecondTime() {
        sequence("uddi_v3.xsd");

        // Name folderName = name("test subfolder");
        // Name fileName = name("test2.txt");
        //
        // Path nodePath = relativePath(XsdLexicon.CONTENT, folderName, fileName, JcrLexicon.CONTENT);
        //
        // Property property = seqtest.getProperty(nodePath, JcrLexicon.DATA);
        // assertThat(property, is(notNullValue()));
        // assertThat(stringFactory.create(property.getFirstValue()), is("This is a test content of file2\n"));
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter01() {
        sequence("definitiveXmlSchema/chapter01.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter03env() {
        sequence("definitiveXmlSchema/chapter03env.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter03ord() {
        sequence("definitiveXmlSchema/chapter03ord.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter03prod() {
        sequence("definitiveXmlSchema/chapter03prod.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter03prod2() {
        sequence("definitiveXmlSchema/chapter03prod.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter04ord1() {
        sequence("definitiveXmlSchema/chapter04ord1.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter04ord2() {
        sequence("definitiveXmlSchema/chapter04ord2.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter04prod() {
        sequence("definitiveXmlSchema/chapter04prod.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter05ord() {
        sequence("definitiveXmlSchema/chapter05ord.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter05prod() {
        sequence("definitiveXmlSchema/chapter05prod.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter07() {
        sequence("definitiveXmlSchema/chapter07.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter08() {
        sequence("definitiveXmlSchema/chapter08.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter09() {
        sequence("definitiveXmlSchema/chapter09.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter11() {
        sequence("definitiveXmlSchema/chapter11.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter13() {
        sequence("definitiveXmlSchema/chapter13.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter14() {
        sequence("definitiveXmlSchema/chapter14.xsd");
        // print = true;
        printOutput();
    }

    @Test
    public void shouldBeAbleToParseXsdFromDefinitiveXmlSchemaExampleChapter15() {
        sequence("definitiveXmlSchema/chapter15.xsd");
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
