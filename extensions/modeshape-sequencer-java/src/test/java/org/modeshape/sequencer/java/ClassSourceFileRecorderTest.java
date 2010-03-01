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
package org.modeshape.sequencer.java;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.java.metadata.JavaMetadata;

public class ClassSourceFileRecorderTest {
    private JavaMetadataSequencer sequencer;
    private InputStream input;
    private MockSequencerOutput output;
    private StreamSequencerContext context;

    @Before
    public void beforeEach() throws Exception {
        sequencer = new JavaMetadataSequencer();
        context = new MockSequencerContext();
        context.getNamespaceRegistry().register("class", "http://www.modeshape.org/sequencer/classfile/1.0");
        context.getNamespaceRegistry().register("java", "http://www.modeshape.org/sequencer/java/1.0");
        context.getNamespaceRegistry().register("nt", "http://www.jcp.org/jcr/nt/1.0");
        output = new MockSequencerOutput(context);
        input = this.getClass().getResourceAsStream("/NodeEntity.javx");
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

    @Test
    public void shouldAllowSettingSourceFileRecorder() throws Exception {
        MockSourceFileRecorder recorder = new MockSourceFileRecorder();
        MockSourceFileRecorder.called = false;

        sequencer.setSourceFileRecorder(recorder);
        sequencer.sequence(input, output, context);

        assertThat(MockSourceFileRecorder.called, is(true));
    }

    @Test
    public void shouldAllowSettingSourceFileRecorderByName() throws Exception {
        MockSourceFileRecorder.called = false;

        sequencer.setSourceFileRecorderClassName(MockSourceFileRecorder.class.getName());
        sequencer.sequence(input, output, context);

        assertThat(MockSourceFileRecorder.called, is(true));
    }

    @Test
    public void shouldAllowSettingSourceFileRecorderToDefaultByName() throws Exception {
        MockSourceFileRecorder.called = false;

        sequencer.setSourceFileRecorderClassName(MockSourceFileRecorder.class.getName());
        sequencer.setSourceFileRecorderClassName(null);
        sequencer.sequence(input, output, context);

        assertThat(MockSourceFileRecorder.called, is(false));
    }

    @Test
    public void shouldAllowSettingSourceFileRecorderToDefault() throws Exception {
        MockSourceFileRecorder.called = false;

        sequencer.setSourceFileRecorder(new MockSourceFileRecorder());
        sequencer.setSourceFileRecorder(null);
        sequencer.sequence(input, output, context);

        assertThat(MockSourceFileRecorder.called, is(false));
    }

    @Test( expected = ClassCastException.class )
    public void shouldNotAllowSettingSourceFileRecorderToInvalidClass() throws Exception {
        sequencer.setSourceFileRecorderClassName(Object.class.getName());
    }

    @Test
    public void shouldParseJavaFileWithClassSourceFileRecorder() throws Exception {
        sequencer.setSourceFileRecorder(new ClassSourceFileRecorder());

        sequencer.sequence(input, output, context);
    }

    public static class MockSourceFileRecorder implements SourceFileRecorder {
        static boolean called = false;

        public void record( StreamSequencerContext context,
                            SequencerOutput output,
                            JavaMetadata javaMetadata ) {
            called = true;
        }
    }
}
