/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.javafile;

import java.io.InputStream;
import javax.jcr.Node;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.api.sequencer.Sequencer;

/**
 * A simple interface that allows an implementer to control how Java source file metadata is mapped to properties (including
 * primary and mixin types) in the graph.
 * <p>
 * Implementations of this class must provide a public, no-argument constructor.
 * </p>
 * <p>
 * To use, supply the implementation class name to a {@link JavaFileSequencer} object. Each instance will be reused for multiple
 * {@link JavaFileSequencer#execute(javax.jcr.Property, javax.jcr.Node, org.modeshape.jcr.api.sequencer.Sequencer.Context)}
 * sequence calls , so implementations of this interface <b>must</b> be thread-safe.
 * </p>
 */
@ThreadSafe
public interface SourceFileRecorder {

    /**
     * Records a source file.
     *
     * @param context the sequencer context (cannot be <code>null</code>)
     * @param inputStream the stream being processed (cannot be <code>null</code>)
     * @param length the length of the java file
     * @param encoding the encoding to use (can be <code>null</code>)
     * @param outputNode the {@link Node} output (cannot be <code>null</code>)
     * @throws Exception if anything fails during the sequencing process
     */
    void record( final Sequencer.Context context,
                 final InputStream inputStream,
                 final long length,
                 final String encoding,
                 final Node outputNode ) throws Exception;

}
