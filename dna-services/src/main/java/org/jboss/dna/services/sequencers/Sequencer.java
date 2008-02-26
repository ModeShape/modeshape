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
import org.jboss.dna.common.monitor.ProgressMonitor;
import net.jcip.annotations.ThreadSafe;

/**
 * The interface for a DNA sequencer, which sequences nodes and their content to extract additional information from the
 * information.
 * <p>
 * Implementations must provide a no-argument constructor.
 * </p>
 * @author Randall Hauch
 */
@ThreadSafe
public interface Sequencer {

    /**
     * This method allows the implementation to initialize and configure itself using the supplied {@link SequencerConfig}
     * information, and is called prior to any calls to {@link #execute(Node, ProgressMonitor) execute}. When this method is
     * called, the implementation must maintain a reference to the supplied configuration (which should then be returned in
     * {@link #getConfiguration()}.
     * <p>
     * Sequencers are always configured before they are executed, and {@link #execute(Node, ProgressMonitor) execute} is called on
     * the return value of the <code>configure</code> method. This provide maximims flexibility: most implementations can simply
     * return <code>this</code>, but more complicated implementations can actually return a different {@link Sequencer}
     * implementation based upon the configuration information. Thus, this <code>configure</code> method is analogous to a
     * factory method, but no separate factory interface is required.
     * </p>
     * <p>
     * @param sequencerConfiguration the configuration for the sequencer
     */
    void setConfiguration( SequencerConfig sequencerConfiguration );

    /**
     * Return the configuration for this sequencer, as supplied to the last {@link #setConfiguration(SequencerConfig)} invocation.
     * @return the configuration, or null if not yet configured
     */
    SequencerConfig getConfiguration();

    /**
     * Execute the sequencing operation on the supplied node, which has recently been created or changed. The implementation of
     * this method is responsible for {@link Session#save() saving} any changes made by this sequencer to the repository content.
     * <p>
     * This operation should report progress to the supplied {@link ProgressMonitor}. This includes the following:
     * <ol>
     * <li>Call {@link ProgressMonitor#beginTask(String, double)} at the beginning of the operation, supplying a useful message
     * describing the operation and a total for the amount of work that will be done by this operation.</li>
     * <li>Report {@link ProgressMonitor#worked(double) work} as work progresses and/or create a
     * {@link ProgressMonitor#createSubtask(double) subtask} and use the ProgressMonitor for the subtask.</li>
     * <li>When finished (whether successfully or not), this operation must call {@link ProgressMonitor#done()}.</li>
     * </ol>
     * The units of work used in these operation is completely up to the implementation.
     * </p>
     * <p>
     * In addition to reporting progress, this operation should also frequently check whether some other caller has requested that
     * the operation be cancelled by calling the {@link ProgressMonitor#isCancelled()} method. If this method returns true, this
     * operation should respect the request by aborting all activities and closing all opened resources.
     * </p>
     * @param node the node that has recently been created or changed; never null
     * @param progress the progress monitor that should be kept updated with the sequencer's progress and that should be
     * frequently consulted as to whether this operation has been {@link ProgressMonitor#isCancelled() cancelled}.
     * @throws RepositoryException
     */
    void execute( Node node, ProgressMonitor progress ) throws RepositoryException;

}
