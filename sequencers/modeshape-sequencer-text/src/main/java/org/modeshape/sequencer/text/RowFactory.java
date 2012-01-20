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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
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
