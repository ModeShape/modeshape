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
package org.modeshape.sequencer.msoffice;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;

public class MSOfficeMetadataSequencerTest {
    private MSOfficeMetadataSequencer sequencer;
    private InputStream input;
    private MockSequencerOutput output;
    private ExecutionContext context;
    private StreamSequencerContext sequencingContext;

    @Before
    public void beforeEach() throws Exception {
        sequencer = new MSOfficeMetadataSequencer();
        context = new ExecutionContext();
        context.getNamespaceRegistry().register("msoffice", "http://www.modeshape.org/msoffice/1.0");
        context.getNamespaceRegistry().register("nt", "http://www.jcp.org/jcr/nt/1.0");
        context.getNamespaceRegistry().register("mix", "http://www.jcp.org/jcr/mix/1.0");
        context.getNamespaceRegistry().register("jcr", "http://www.jcp.org/jcr/1.0");
    }

    @After
    public void afterEach() throws Exception {
        if (input != null) {
            try {
                input.close();
            } finally {
                input = null;
            }
        }
    }

    protected InputStream getTestDocument( String resourcePath ) {
        InputStream result = this.getClass().getResourceAsStream("/" + resourcePath);
        assertThat(result, is(notNullValue()));
        return result;
    }

    @Test
    public void shouldSequenceCommaDelimitedFileWithOneLine() {
        input = getTestDocument("word.doc");

        Path inputPath = context.getValueFactories().getPathFactory().create("/files/word.doc");
        Set<Property> props = new HashSet<Property>();
        String mimeType = "application/msword";
        sequencingContext = new StreamSequencerContext(context, inputPath, props, mimeType, new SimpleProblems());
        output = new MockSequencerOutput(sequencingContext);

        sequencer.sequence(input, output, sequencingContext);
        if (sequencingContext.getProblems().hasProblems()) {
            System.out.println("Problems sequencing \"word.doc\"");
        }
        assertThat(sequencingContext.getProblems().hasProblems(), is(false));

        System.out.println(output);

        assertThat(output.getProperty("word.doc/msoffice:metadata", "msoffice:author").getFirstValue(),
                   is((Object)"Michael Trezzi"));
    }
}
