/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.services.sequencers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * The interface for a DNA sequencer, which sequences nodes and their content to extract additional information from the
 * information.
 * <p>
 * Implementations must provide a no-argument constructor.
 * </p>
 * @author Randall Hauch
 */
public interface ISequencer {

    /**
     * This method allows the implementation to initialize and configure itself using the supplied {@link SequencerConfig}
     * information, and is called prior to {@link #execute(Node) execute}.
     * <p>
     * Sequencers are always configured before they are executed, and {@link #execute(Node) execute} is called on the return value
     * of the <code>configure</code> method. This provide maximims flexibility: most implementations can simply return
     * <code>this</code>, but more complicated implementations can actually return a different {@link ISequencer}
     * implementation based upon the configuration information. Thus, this <code>configure</code> method is analogous to a
     * factory method, but no separate factory interface is required.
     * </p>
     * @param sequencerConfiguration the configuration for the sequencer
     * @return the configured sequencer; may not be null
     */
    ISequencer configure( SequencerConfig sequencerConfiguration );

    /**
     * Execute the sequencing operation on the supplied node, which has recently been created or changed. The implementation of
     * this method is responsible for {@link Session#save() saving} any changes made by this sequencer to the repository content.
     * @param node the node that has recently been created or changed; never null
     * @throws RepositoryException
     */
    void execute( Node node ) throws RepositoryException;

}
