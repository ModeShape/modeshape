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
 * {@link ClassFileSequencer#execute(javax.jcr.Property, javax.jcr.Node, org.modeshape.jcr.api.sequencer.Sequencer.Context) sequence calls}, so
 * implementations of this interface <b>must</b> be thread-safe.
 * </p>
 */
@ThreadSafe
public interface ClassFileRecorder {

    /**
     * Records a row using the provided {@link Node} node.
     *
     * @param context the {@link org.modeshape.jcr.api.sequencer.Sequencer.Context}
     * @param outputNode the node to which the new content should be sequenced.
     * This may either be new or existent {@link org.modeshape.jcr.api.sequencer.Sequencer#execute}
     * @param classMetadata the metadata for the class file
     * @throws javax.jcr.RepositoryException if anything fails during the recording process
     */
    void recordClass( Sequencer.Context context, Node outputNode, ClassMetadata classMetadata ) throws RepositoryException;
}
