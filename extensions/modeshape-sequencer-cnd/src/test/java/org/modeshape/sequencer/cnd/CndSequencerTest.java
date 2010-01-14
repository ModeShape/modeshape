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
package org.modeshape.sequencer.cnd;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class CndSequencerTest {
    private CndSequencer sequencer;
    private InputStream content;
    private MockSequencerOutput output;
    private URL cndEmpty;
    private URL cndImages;
    private URL cndMp3;
    private URL cndBuiltIns;
    private URL standardDdl;
    private StreamSequencerContext context;

    @Before
    public void beforeEach() {
        sequencer = new CndSequencer();
        context = new MockSequencerContext("/a/mySequencer.cnd");
        context.getNamespaceRegistry().register("jcr", "http://www.jcp.org/jcr/1.0");
        context.getNamespaceRegistry().register("nt", "http://www.jcp.org/jcr/nt/1.0");
        context.getNamespaceRegistry().register("mix", "http://www.jcp.org/jcr/mix/1.0");
        output = new MockSequencerOutput(context);
        cndEmpty = this.getClass().getClassLoader().getResource("empty.cnd");
        cndImages = this.getClass().getClassLoader().getResource("images.cnd");
        cndMp3 = this.getClass().getClassLoader().getResource("mp3.cnd");
        cndBuiltIns = this.getClass().getClassLoader().getResource("builtin_nodetypes.cnd");
        standardDdl = this.getClass().getClassLoader().getResource("StandardDdl.cnd");
    }

    @After
    public void afterEach() throws Exception {
        if (content != null) {
            try {
                content.close();
            } finally {
                content = null;
            }
        }
    }

    @Test
    public void shouldGenerateNodeTypesForCndFileWithJSR170BuiltIns() throws IOException {
        URL url = this.cndBuiltIns;
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);
        // assertThat(output.getPropertyValues("image:metadata", "jcr:primaryType"), is(new Object[] {"image:metadata"}));
        // assertThat(output.getPropertyValues("image:metadata", "jcr:mimeType"), is(new Object[] {"image/jpeg"}));
        // assertThat(output.getPropertyValues("image:metadata", "image:formatName"), is(new Object[] {"JPEG"}));
        // assertThat(output.getPropertyValues("image:metadata", "image:width"), is(new Object[] {48}));
        // assertThat(output.getPropertyValues("image:metadata", "image:height"), is(new Object[] {48}));
        // assertThat(output.getPropertyValues("image:metadata", "image:bitsPerPixel"), is(new Object[] {24}));
        // assertThat(output.getPropertyValues("image:metadata", "image:progressive"), is(new Object[] {false}));
        // assertThat(output.getPropertyValues("image:metadata", "image:numberOfImages"), is(new Object[] {1}));
        // assertThat(output.getPropertyValues("image:metadata", "image:physicalWidthDpi"), is(new Object[] {72}));
        // assertThat(output.getPropertyValues("image:metadata", "image:physicalHeightDpi"), is(new Object[] {72}));
        // assertThat(((Float)(output.getPropertyValues("image:metadata", "image:physicalWidthInches")[0])).doubleValue(),
        // is(closeTo(0.666667d, 0.0001d)));
        // assertThat(((Float)(output.getPropertyValues("image:metadata", "image:physicalHeightInches")[0])).doubleValue(),
        // is(closeTo(0.666667d, 0.0001d)));
    }

    @Test
    public void shouldGenerateNodeTypesForEmptyCndFile() throws IOException {
        URL url = this.cndEmpty;
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);
    }

    @Test
    public void shouldGenerateNodeTypesForCndFileWithImageNodeTypes() throws IOException {
        URL url = this.cndImages;
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);
    }

    @Test
    public void shouldGenerateNodeTypesForCndFileWithMp3NodeTypes() throws IOException {
        URL url = this.cndMp3;
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);
    }
    
    @Test
    public void shouldGenerateNodeTypesForCndFileWithDdlTypes() throws IOException {
        URL url = this.standardDdl;
        assertThat(url, is(notNullValue()));
        content = url.openStream();
        assertThat(content, is(notNullValue()));
        sequencer.sequence(content, output, context);
    }

}
