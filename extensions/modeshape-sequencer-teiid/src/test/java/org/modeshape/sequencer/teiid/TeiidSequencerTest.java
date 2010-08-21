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
package org.modeshape.sequencer.teiid;

import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * 
 */
public class TeiidSequencerTest {
    private ModelSequencer sequencer;
    private InputStream content;
    private MockSequencerOutput output;
    private StreamSequencerContext context;

    @Before
    public void beforeEach() {
        sequencer = new ModelSequencer();
        context = new MockSequencerContext("/a/mySequencer.cnd");
        context.getNamespaceRegistry().register("jcr", "http://www.jcp.org/jcr/1.0");
        context.getNamespaceRegistry().register("nt", "http://www.jcp.org/jcr/nt/1.0");
        context.getNamespaceRegistry().register("mix", "http://www.jcp.org/jcr/mix/1.0");
        output = new MockSequencerOutput(context);
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
    public void shouldGenerateNodeTypesForEmptyCndFile() throws IOException {
        // assertThat(url, is(notNullValue()));
        // content = url.openStream();
        // assertThat(content, is(notNullValue()));
        // sequencer.sequence(content, output, context);
    }

}
