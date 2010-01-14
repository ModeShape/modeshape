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
package org.modeshape.sequencer.classfile;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.classfile.metadata.ClassMetadata;
import org.modeshape.sequencer.classfile.metadata.EnumMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClassFileSequencerTest {
    private ClassFileSequencer sequencer;
    private InputStream input;
    private MockSequencerOutput output;
    private StreamSequencerContext context;

    @Before
    public void beforeEach() throws Exception {
        sequencer = new ClassFileSequencer();
        context = new MockSequencerContext();
        context.getNamespaceRegistry().register("class", "http://www.modeshape.org/sequencer/classfile/1.0");
        context.getNamespaceRegistry().register("nt", "http://www.jcp.org/jcr/nt/1.0");
        output = new MockSequencerOutput(context);
        input = this.getClass().getResourceAsStream("/NodeEntity.clazz");
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
    public void shouldAllowSettingClassFileRecorder() throws Exception {
        MockClassFileRecorder recorder = new MockClassFileRecorder();
        MockClassFileRecorder.called = false;

        sequencer.setClassFileRecorder(recorder);
        sequencer.sequence(input, output, context);

        assertThat(MockClassFileRecorder.called, is(true));
    }

    @Test
    public void shouldAllowSettingClassFileRecorderByName() throws Exception {
        MockClassFileRecorder.called = false;

        sequencer.setClassFileRecorderClassName(MockClassFileRecorder.class.getName());
        sequencer.sequence(input, output, context);

        assertThat(MockClassFileRecorder.called, is(true));
    }

    @Test
    public void shouldAllowSettingClassFileRecorderToDefaultByName() throws Exception {
        MockClassFileRecorder.called = false;

        sequencer.setClassFileRecorderClassName(MockClassFileRecorder.class.getName());
        sequencer.setClassFileRecorderClassName(null);
        sequencer.sequence(input, output, context);

        assertThat(MockClassFileRecorder.called, is(false));
    }

    @Test
    public void shouldAllowSettingClassFileRecorderToDefault() throws Exception {
        MockClassFileRecorder.called = false;

        sequencer.setClassFileRecorder(new MockClassFileRecorder());
        sequencer.setClassFileRecorder(null);
        sequencer.sequence(input, output, context);

        assertThat(MockClassFileRecorder.called, is(false));
    }

    @Test( expected = ClassCastException.class )
    public void shouldNotAllowSettingClassFileRecorderToInvalidClass() throws Exception {
        sequencer.setClassFileRecorderClassName(Object.class.getName());
    }


    public static class MockClassFileRecorder implements ClassFileRecorder {
        static boolean called = false;

        public void recordClass( StreamSequencerContext context,
                                 SequencerOutput output,
                                 ClassMetadata classMetadata ) {
            called = true;
        }

        /**
         * @param context
         * @param output
         * @param enumMetadata
         */
        public void recordEnum( StreamSequencerContext context,
                                SequencerOutput output,
                                EnumMetadata enumMetadata ) {
        }
    }
}
