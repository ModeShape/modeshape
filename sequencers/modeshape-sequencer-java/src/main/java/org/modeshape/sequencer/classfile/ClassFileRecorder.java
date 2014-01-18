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
package org.modeshape.sequencer.classfile;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.classfile.metadata.ClassMetadata;

/**
 * A simple interface that allows an implementer to control how class file metadata mapped to properties (including primary and
 * mixin types) in the graph.
 * <p>
 * Implementations of this class must provide a public, no-argument constructor.
 * </p>
 * <p>
 * To use, supply the implementation class name to a {@link ClassFileSequencer} object. Each instance will be reused for multiple
 * {@link ClassFileSequencer#execute(javax.jcr.Property, javax.jcr.Node, org.modeshape.jcr.api.sequencer.Sequencer.Context)
 * sequence calls}, so implementations of this interface <b>must</b> be thread-safe.
 * </p>
 */
@ThreadSafe
public interface ClassFileRecorder {

    /**
     * Records a row using the provided {@link Node} node.
     * 
     * @param context the {@link org.modeshape.jcr.api.sequencer.Sequencer.Context}
     * @param outputNode the node to which the new content should be sequenced. This may either be new or existent
     *        {@link org.modeshape.jcr.api.sequencer.Sequencer#execute}
     * @param classMetadata the metadata for the class file
     * @throws javax.jcr.RepositoryException if anything fails during the recording process
     */
    void recordClass( Sequencer.Context context,
                      Node outputNode,
                      ClassMetadata classMetadata ) throws RepositoryException;
}
