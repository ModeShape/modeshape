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

import java.io.IOException;
import org.junit.Test;
import org.modeshape.graph.sequencer.AbstractStreamSequencerTest;
import org.modeshape.graph.sequencer.StreamSequencer;

/**
 * 
 */
public class CndSequencerTest extends AbstractStreamSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.AbstractStreamSequencerTest#createSequencer()
     */
    @Override
    protected StreamSequencer createSequencer() {
        return new CndSequencer();
    }

    @Test
    public void shouldGenerateNodeTypesForCndFileWithJSR170BuiltIns() throws IOException {
        sequence("builtin_nodetypes.cnd");
        assertNoProblems();
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
        sequence("empty.cnd");
        assertNoProblems();
    }

    @Test
    public void shouldGenerateNodeTypesForCndFileWithImageNodeTypes() throws IOException {
        sequence("images.cnd");
        assertNoProblems();
    }

    @Test
    public void shouldGenerateNodeTypesForCndFileWithMp3NodeTypes() throws IOException {
        sequence("mp3.cnd");
        assertNoProblems();
    }

    @Test
    public void shouldGenerateNodeTypesForCndFileWithDdlTypes() throws IOException {
        sequence("StandardDdl.cnd");
        assertNoProblems();
    }

}
