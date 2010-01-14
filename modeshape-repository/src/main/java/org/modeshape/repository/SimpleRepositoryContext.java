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
package org.modeshape.repository;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.observe.Observer;

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
     * @see org.modeshape.graph.connector.RepositoryContext#getConfiguration(int)
     */
    public Subgraph getConfiguration( int depth ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryContext#getExecutionContext()
     */
    public ExecutionContext getExecutionContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryContext#getObserver()
     */
    public Observer getObserver() {
        return observer;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositoryContext#getRepositoryConnectionFactory()
     */
    public RepositoryConnectionFactory getRepositoryConnectionFactory() {
        return connectionFactory;
    }

}
