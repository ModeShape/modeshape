/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.graph.io;

import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.NodeConflictBehavior;
import org.modeshape.graph.Graph.Batch;
import org.modeshape.graph.Graph.Create;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * A {@link Destination} that makes the changes to a graph via a {@link Batch}.
 */
@NotThreadSafe
public class GraphBatchDestination implements Destination {
    protected final Graph.Batch batch;
    protected final boolean ignoreSubmit;

    /**
     * Create a new instance that will use the specified batch. When {@link #submit()} is called, the batch will be
     * {@link Batch#execute() executed}.
     * 
     * @param batch the batch
     * @throws IllegalArgumentException if the batch is null
     */
    public GraphBatchDestination( Graph.Batch batch ) {
        this(batch, false);
    }

    /**
     * Create a new instance that will use the specified batch. If {@code ignoreSubmit} is true, then {@link #submit()} does
     * nothing (and the batch must be executed}; otherwise, {@link #submit()} immediately calls the {@link Batch#execute()
     * executed}.
     * 
     * @param batch the batch
     * @param ignoreSubmit true if the {@link #submit()} method should be ignored, or false otherwise
     * @throws IllegalArgumentException if the batch is null
     */
    public GraphBatchDestination( Graph.Batch batch,
                                  boolean ignoreSubmit ) {
        assert batch != null;
        this.batch = batch;
        this.ignoreSubmit = ignoreSubmit;
    }

    /**
     * Return whether this instance is ignoring calls to {@link #submit()}.
     * 
     * @return ignoreSubmit
     */
    public boolean isSubmitIgnored() {
        return ignoreSubmit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.io.Destination#getExecutionContext()
     */
    public ExecutionContext getExecutionContext() {
        return batch.getGraph().getContext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.io.Destination#create(org.modeshape.graph.property.Path, Iterable)
     */
    public void create( Path path,
                        Iterable<Property> properties ) {
        assert properties != null;
        Create<Batch> create = batch.create(path, properties);
        assert create != null;
        NodeConflictBehavior behavior = createBehaviorFor(path);
        if (behavior != null) {
            switch (behavior) {
                case APPEND:
                    create.byAppending();
                    break;
                case REPLACE:
                    create.orReplace();
                    break;
                case UPDATE:
                    create.byAppending();
                    break;
                case DO_NOT_REPLACE:
                    create.byAppending();
                    break;
            }
        }
        create.and();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.io.Destination#create(org.modeshape.graph.property.Path, org.modeshape.graph.property.Property,
     *      org.modeshape.graph.property.Property[])
     */
    public void create( Path path,
                        Property firstProperty,
                        Property... additionalProperties ) {
        Create<Batch> create = null;
        if (firstProperty == null) {
            create = batch.create(path);
        } else {
            create = batch.create(path, firstProperty, additionalProperties);
        }
        assert create != null;
        NodeConflictBehavior behavior = createBehaviorFor(path);
        if (behavior != null) {
            switch (behavior) {
                case APPEND:
                    create.byAppending();
                    break;
                case REPLACE:
                    create.orReplace();
                    break;
                case UPDATE:
                    create.byAppending();
                    break;
                case DO_NOT_REPLACE:
                    create.byAppending();
                    break;
            }
        }
        create.and();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.io.Destination#setProperties(org.modeshape.graph.property.Path,
     *      org.modeshape.graph.property.Property[])
     */
    public void setProperties( Path path,
                               Property... properties ) {
        if (properties == null) return;

        batch.set(properties).on(path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.io.Destination#submit()
     */
    public void submit() {
        // Execute only if we're not ignoring submits ...
        if (!this.ignoreSubmit && !batch.hasExecuted()) batch.execute();
    }

    /**
     * Override this method in a subclass to control the {@link NodeConflictBehavior} that should be used when creating the node
     * at the supplied path. By default, this method returns null.
     * 
     * @param path the path of the new node
     * @return the conflict behavior, or null if {@link NodeConflictBehavior#UPDATE} should be used
     */
    protected NodeConflictBehavior createBehaviorFor( Path path ) {
        return null;
    }
}
