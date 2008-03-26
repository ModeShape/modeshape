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
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.component.Component;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.services.ExecutionContext;

/**
 * The interface for a DNA sequencer, which sequences nodes and their content to extract additional information from the
 * information.
 * <p>
 * Implementations must provide a no-argument constructor.
 * </p>
 * @author Randall Hauch
 */
@ThreadSafe
public interface Sequencer extends Component<SequencerConfig> {

    /**
     * Execute the sequencing operation on the supplied node, which has recently been created or changed. The implementation of
     * this method is responsible for {@link Session#save() saving} any changes made by this sequencer to the repository content.
     * <p>
     * This operation should report progress to the supplied {@link ProgressMonitor}. At the beginning of the operation, call
     * {@link ProgressMonitor#beginTask(double, org.jboss.dna.common.i18n.I18n, Object...)} with a meaningful message describing
     * the operation and a total for the amount of work that will be done by this sequencer. Then perform the sequencing work,
     * periodically reporting work by specifying the {@link ProgressMonitor#worked(double) amount of work} that has was just
     * completed or by {@link ProgressMonitor#createSubtask(double) creating a subtask} and reporting work against that subtask
     * monitor.
     * </p>
     * <p>
     * The implementation should also periodically check whether the operation has been
     * {@link ProgressMonitor#isCancelled() cancelled}. If this method returns true, the implementation should abort all work as
     * soon as possible and close any resources that were acquired or opened.
     * </p>
     * <p>
     * Finally, the implementation should call {@link ProgressMonitor#done()} when the operation has finished.
     * </p>
     * @param input the node that has recently been created or changed; never null
     * @param output the node at which the sequencing content should be placed; never null, but possible the same as
     * <code>input</code>
     * @param context the context in which this sequencer is executing; never null
     * @param progress the progress monitor that should be kept updated with the sequencer's progress and that should be
     * frequently consulted as to whether this operation has been {@link ProgressMonitor#isCancelled() cancelled}.
     * @throws RepositoryException
     */
    void execute( Node input, Node output, ExecutionContext context, ProgressMonitor progress ) throws RepositoryException;

}
