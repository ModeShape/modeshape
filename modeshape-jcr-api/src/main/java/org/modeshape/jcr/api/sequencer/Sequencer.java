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
package org.modeshape.jcr.api.sequencer;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * A component that reads recently-changed content (often uploaded files) and extracts additional information from the content.
 * <p>
 * Each ModeShape repository can be configured with zero or more sequencers. Each sequencer is configured with a set of match
 * conditions that define the acceptable patterns for the paths of changed nodes, as well as a path specification that defines
 * where the derived (or generated) output should be placed. Then when clients change nodes with paths that satisfy the match
 * conditions, the repository will create a new Session and invoke the sequencer, which is then expected to process the changed
 * content and generate the derived information under the supplied parent node. The session will be saved automatically or, if an
 * exception is thrown, discard the changes and close the session.
 * </p>
 */
public abstract class Sequencer {

    private String name;
    private String description;
    private Object[] pathExpressions;
    private String pathExpression;

    /**
     * Get the name of this sequencer.
     * 
     * @return the sequencer name; null only if not {@link #initialize(String, String) initialized}
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description for this sequencer.
     * 
     * @return the description, or null if there is no description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return pathExpression
     */
    public String getPathExpression() {
        return pathExpression;
    }

    /**
     * @return pathExpressions
     */
    public Object[] getPathExpressions() {
        return pathExpressions;
    }

    /**
     * Initialize the sequencer. This is called automatically by ModeShape, and should not be called by the sequencer.
     * <p>
     * This method can be overridden by implementations to do a one-time initialization of any internal components. This method is
     * invoked during first {@link #initialize} invocation, which is done automatically by ModeShape upon repository
     * intialization.
     * 
     * @param sequencerName the name of the sequencer, which can be used for logging or exception purposes
     * @param repositoryName the name of the repository, which can be used for logging or exception purposes
     * @throws InvalidSequencerPathExpression if any of the path expressions are invalid
     */
    public void initialize( String sequencerName,
                            String repositoryName ) throws InvalidSequencerPathExpression {
    }

    /**
     * Execute the sequencing operation on the supplied node, which has recently been created or changed. The implementation of
     * this method is responsible for modifying the appropriate nodes under the supplied <code>parentOfOutput</code> node and
     * closing all acquired resources, even in the case of exceptions.
     * <p>
     * It is possible that a sequencer is configured to apply to zero, one, or multiple properties on a node. In the case of one
     * property, that property will be passed into the method; in other cases, the <code>changedProperty</code> will be null.
     * </p>
     * 
     * @param changedNode the node that was changed and that typically contains the input for the sequencer; never null
     * @param changedProperty the property that was changed; may be null if the sequencer's match conditions did not specify a
     *        property, but if not null will always be a property on <code>changedNode</code>
     * @param parentOfOutput the parent node under which the sequencer is to place the generated/derived output; never null, but
     *        possibly in a different workspace than the <code>changedNode</code>
     * @param context the context in which this sequencer is executing; never null
     * @throws SequencerException if there is an error in this sequencer
     * @throws RepositoryException if there is an error accessing the repository
     */
    public abstract void execute( Node changedNode,
                                  Property changedProperty,
                                  Node parentOfOutput,
                                  SequencerContext context ) throws SequencerException, RepositoryException;

    @Override
    public String toString() {
        return name + (description != null ? (" : " + description) : "");
    }
}
