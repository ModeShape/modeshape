/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.io;

import java.util.HashSet;
import java.util.Set;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.sequencer.SequencerOutput;

/**
 * Utility for wrapping sequencer output to commit to a graph.
 * <p>
 * Constructors allow providing either a {@link Graph} or a {@link Graph.Batch} object. If {@link Graph} constructor is used, a
 * {@link Graph.Batch} object is created to utilize the batching capabilities of all graph requests.
 * <p>
 * Calling close() commits all batched graph requests.
 */
public class GraphSequencerOutput implements SequencerOutput {

    private final Graph.Batch batch;

    private final PathFactory pathFactory;

    private final Set<Path> paths = new HashSet<Path>();

    /**
     * Create a graph sequencer output instance using {@link Graph.Batch} object.
     * 
     * @param batch the {@link Graph.Batch} object; may not be null
     */
    public GraphSequencerOutput( Graph.Batch batch ) {
        super();
        this.batch = batch;
        ExecutionContext context = batch.getGraph().getContext();
        this.pathFactory = context.getValueFactories().getPathFactory();
    }

    /**
     * Create a graph sequencer output instance using {@link Graph} object. A {@link Graph.Batch} object is created as a result.
     * 
     * @param graph the {@link Graph} object; may not be null
     */
    public GraphSequencerOutput( Graph graph ) {
        this(graph.batch());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencer.SequencerOutput#setProperty(java.lang.String, java.lang.String, java.lang.Object[])
     */
    public void setProperty( String nodePath,
                             String propertyName,
                             Object... values ) {
        Path path = pathFactory.create(nodePath);
        if (paths.add(path)) {
            batch.create(path).and();
        }
        batch.set(propertyName).on(path).to(values);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencer.SequencerOutput#setProperty(org.jboss.dna.graph.property.Path,
     *      org.jboss.dna.graph.property.Name, java.lang.Object[])
     */
    public void setProperty( Path nodePath,
                             Name propertyName,
                             Object... values ) {
        if (paths.add(nodePath)) {
            batch.create(nodePath).and();
        }
        batch.set(propertyName).on(nodePath).to(values);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencer.SequencerOutput#setReference(java.lang.String, java.lang.String, java.lang.String[])
     */
    public void setReference( String nodePath,
                              String propertyName,
                              String... paths ) {
        Path path = pathFactory.create(nodePath);
        if (this.paths.add(path)) {
            batch.create(path).and();
        }
        batch.set(propertyName).on(nodePath).to(paths);
    }

    public void close() {
        batch.execute();
    }

}
