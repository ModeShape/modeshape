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
package org.modeshape.graph.sequencer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.junit.Before;
import org.modeshape.common.collection.Problem;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Path;

/**
 * An abstract test class for {@link StreamSequencer} implementations.
 */
public abstract class AbstractStreamSequencerTest {
    protected ExecutionContext context;
    protected StreamSequencer sequencer;
    protected MockSequencerOutput output;
    protected StreamSequencerContext sequencerContext;
    protected boolean print = false;

    @Before
    public void beforeEach() {
        print = false;
        context = new ExecutionContext();
        sequencer = createSequencer();
    }

    /**
     * Create the StreamSequencer instance that should be tested.
     * 
     * @return the sequencer instance; may not be null
     */
    protected abstract StreamSequencer createSequencer();

    /**
     * Create a path given the supplied string representation.
     * 
     * @param path the string form of the path
     * @return the path
     */
    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected InputStream content( String pathOfResourceOnClasspath ) throws IOException {
        URL url = getClass().getClassLoader().getResource(pathOfResourceOnClasspath);
        assertThat(url, is(notNullValue()));
        InputStream stream = url.openStream();
        assertThat(stream, is(notNullValue()));
        return stream;
    }

    /**
     * Sequence the file on the classpath at the supplied path. The sequencing is done using the {@link #sequencerContext}, the
     * output is placed into the {@link #output}, the stream to the classpath resource is closed.
     * 
     * @param pathOfResourceOnClasspath
     * @throws IOException
     */
    protected void sequence( String pathOfResourceOnClasspath ) throws IOException {
        InputStream content = content(pathOfResourceOnClasspath);
        try {
            sequencerContext = createSequencerContext(pathOfResourceOnClasspath);
            output = new MockSequencerOutput(sequencerContext, true);
            sequencer.sequence(content, output, sequencerContext);
        } finally {
            if (content != null) {
                try {
                    content.close();
                } finally {
                    content = null;
                }
            }
        }
    }

    protected StreamSequencerContext createSequencerContext( String pathStr ) {
        return new StreamSequencerContext(context, path("/" + pathStr), null, null, new SimpleProblems());
    }

    protected void assertNoProblems() {
        assertThat("Must be called only after 'sequence(String)' is called", sequencerContext, is(notNullValue()));
        Problems problems = sequencerContext.getProblems();
        if (!sequencerContext.getProblems().isEmpty()) {
            for (Problem problem : problems) {
                System.out.println(problem);
            }
        }
        assertThat(sequencerContext.getProblems().isEmpty(), is(true));
    }

    protected void printOutput() {
        if (print) {
            System.out.println(output);
        }
    }
}
