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

import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.classfile.metadata.ClassMetadata;

/**
 * A simple interface that allows an implementer to control how class file metadata mapped to properties (including primary and
 * mixin types) in the graph.
 * <p>
 * Implementations of this class must provide a public, no-argument constructor.
 * </p>
 * <p>
 * To use, supply the implementation class name to a {@link ClassFileSequencer} object. Each instance will be reused for multiple
 * {@link ClassFileSequencer#sequence(java.io.InputStream, SequencerOutput, StreamSequencerContext) sequence calls}, so
 * implementations of this interface <b>must</b> be thread-safe.
 * </p>
 */
@ThreadSafe
public interface ClassFileRecorder {

    /**
     * Records a row using the provided {@link SequencerOutput} instance.
     * 
     * @param context the sequencer context
     * @param output the {@link StreamSequencer} output
     * @param classMetadata the metadata for the class file
     */
    void recordClass( StreamSequencerContext context,
                      SequencerOutput output,
                      ClassMetadata classMetadata );
}
