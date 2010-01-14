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
package org.modeshape.repository.sequencer;

import java.util.Set;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.component.Component;
import org.modeshape.graph.Node;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.observe.NetChangeObserver.NetChange;
import org.modeshape.repository.RepositoryLibrary;
import org.modeshape.repository.util.RepositoryNodePath;

/**
 * The interface for a ModeShape sequencer, which sequences nodes and their content to extract additional information from the
 * information.
 * <p>
 * Implementations must provide a no-argument constructor.
 * </p>
 */
@ThreadSafe
public interface Sequencer extends Component<SequencerConfig> {

    /**
     * Execute the sequencing operation on the supplied node, which has recently been created or changed. The implementation of
     * this method is responsible for modifying the appropriate nodes and {@link Destination#submit() saving} any changes made by
     * this sequencer, and closing any other acquired resources, even in the case of exceptions.
     * <p>
     * The {@link SequencingService} determines the sequencers that should be executed by monitoring the changes to one or more
     * workspaces (it registers an {@link Observer} with the {@link RepositoryLibrary}). Changes in those workspaces are
     * aggregated for each transaction, and organized into {@link NetChange changes for each node}. The SequencingService then
     * determines for each {@link NetChange set of changes to a node} the set of full paths to the properties that have changed
     * and whether those paths {@link SequencerPathExpression#matcher(String) match} the sequencer's
     * {@link SequencerConfig#getPathExpressions() path expressions}. Each path expression produces the path to the output node,
     * and these output paths are accumulated and (with the original node that changed, the node change summary, and other
     * information) supplied to the sequencer via this method.
     * <p>
     * It is possible that a sequencer is configured to apply to multiple properties on a node. So, in cases where multiple
     * properties are changed on a single node (within a single repository transaction), the sequencer will only be executed once.
     * Also, in such cases the sequencer's configuration may imply multiple output nodes, so it is left to the sequencer to define
     * the behavior in such cases.
     * </p>
     * 
     * @param input the node that has recently been created or changed; never null
     * @param sequencedPropertyName the name of the property that caused this sequencer to be executed; never null and never empty
     * @param changes the immutable summary of changes that occurred on the <code>input</code> node within the transaction; never
     *        null
     * @param outputPaths the paths to the nodes where the sequencing content should be placed; never null and never empty, but
     *        the set may contain paths for non-existent nodes or may reference the <code>input</code> node
     * @param context the context in which this sequencer is executing; never null
     * @param problems the interface used for recording problems; never null
     * @throws SequencerException if there is an error in this sequencer
     */
    void execute( Node input,
                  String sequencedPropertyName,
                  NetChange changes,
                  Set<RepositoryNodePath> outputPaths,
                  SequencerContext context,
                  Problems problems ) throws SequencerException;

}
