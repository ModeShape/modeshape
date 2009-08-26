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
package org.jboss.dna.repository;

import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.observe.Observer;

/**
 * A simple, immutable {@link RepositoryContext} implementation that uses the references supplied as parameters to the
 * constructor.
 */
@Immutable
public class SimpleRepositoryContext implements RepositoryContext {

    private final ExecutionContext context;
    private final Observer observer;
    private final RepositoryConnectionFactory connectionFactory;

    public SimpleRepositoryContext( ExecutionContext context,
                                    Observer observer,
                                    RepositoryConnectionFactory connectionFactory ) {
        this.context = context;
        this.observer = observer;
        this.connectionFactory = connectionFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryContext#getConfiguration(int)
     */
    public Subgraph getConfiguration( int depth ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryContext#getExecutionContext()
     */
    public ExecutionContext getExecutionContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryContext#getObserver()
     */
    public Observer getObserver() {
        return observer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryContext#getRepositoryConnectionFactory()
     */
    public RepositoryConnectionFactory getRepositoryConnectionFactory() {
        return connectionFactory;
    }

}
