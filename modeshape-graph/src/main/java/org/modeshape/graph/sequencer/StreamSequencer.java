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

import java.io.InputStream;

/**
 * The interface for a ModeShape sequencer that processes a property as a stream to extract information from the content and store
 * in the repository.
 * <p>
 * ModeShape creates a new StreamSequencer instance each time a sequencing operation is to be performed on some input data.
 * Therefore, implementations must provide a no-argument constructor. Implementations may also define Java-bean style property
 * setters that will be used to pass sequencer configuration properties to the sequencer instance.
 * </p>
 */
public interface StreamSequencer {

    /**
     * Sequence the data found in the supplied stream, placing the output information into the supplied map.
     * <p>
     * ModeShape's SequencingService determines the sequencers that should be executed by monitoring the changes to one or more
     * workspaces that it is monitoring. Changes in those workspaces are aggregated and used to determine which sequencers should
     * be called. If the sequencer implements this interface, then this method is called with the property that is to be sequenced
     * along with the interface used to register the output. The framework takes care of all the rest.
     * </p>
     * 
     * @param stream the stream with the data to be sequenced; never <code>null</code>
     * @param output the output from the sequencing operation; never <code>null</code>
     * @param context the context for the sequencing operation; never <code>null</code>
     */
    void sequence( InputStream stream,
                   SequencerOutput output,
                   StreamSequencerContext context );
}
