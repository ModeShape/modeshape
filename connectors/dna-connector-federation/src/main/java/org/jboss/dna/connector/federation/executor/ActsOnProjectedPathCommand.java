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
package org.jboss.dna.connector.federation.executor;

import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.commands.ActsOnPath;
import org.jboss.dna.spi.graph.commands.GraphCommand;

/**
 * @author Randall Hauch
 * @param <T> the command type
 */
public class ActsOnProjectedPathCommand<T extends GraphCommand> implements ActsOnPath, GraphCommand {

    private final T delegate;
    private final Path projectedPath;

    protected ActsOnProjectedPathCommand( T delegate,
                                          Path projectedPath ) {
        assert delegate != null;
        assert projectedPath != null;
        this.delegate = delegate;
        this.projectedPath = projectedPath;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.ActsOnPath#getPath()
     */
    public Path getPath() {
        return projectedPath;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GraphCommand#getError()
     */
    public Throwable getError() {
        return delegate.getError();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GraphCommand#hasError()
     */
    public boolean hasError() {
        return delegate.hasError();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GraphCommand#hasNoError()
     */
    public boolean hasNoError() {
        return delegate.hasNoError();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GraphCommand#isCancelled()
     */
    public boolean isCancelled() {
        return delegate.isCancelled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.commands.GraphCommand#setError(java.lang.Throwable)
     */
    public void setError( Throwable error ) {
        delegate.setError(error);
    }

    /**
     * @return delegate
     */
    protected T getOriginalCommand() {
        return delegate;
    }
}
