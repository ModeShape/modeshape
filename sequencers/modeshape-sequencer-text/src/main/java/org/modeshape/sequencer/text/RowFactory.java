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
package org.modeshape.sequencer.text;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.NotThreadSafe;


/**
 * A simple interface that allows an implementer to control how rows in a text file are mapped to properties (including primary
 * and mixin types) in the graph.
 * <p>
 * Implementations of this class must provide a public, no-argument constructor.
 * </p>
 * <p>
 * To use, supply the implementation class name to a {@link AbstractTextSequencer} object. New instances are created for each
 * {@link org.modeshape.jcr.api.sequencer.Sequencer#execute(javax.jcr.Property, javax.jcr.Node, org.modeshape.jcr.api.sequencer.Sequencer.Context)}
 * so implementations of this interface need not be thread-safe.
 * </p>
 */
@NotThreadSafe
public interface RowFactory {

    /**
     * Records a row using under the provided {@link Node} instance.
     * 
     * @param outputNode the node under which the sequencing output is placed.
     * @param columns the columns that could be parsed out for the row
     * @throws javax.jcr.RepositoryException if anything fails while recording the row under the ouput node.
     */
    void recordRow( Node outputNode, String[] columns ) throws RepositoryException;

}
