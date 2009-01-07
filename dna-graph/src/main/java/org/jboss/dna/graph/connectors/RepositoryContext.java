/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.connectors;

import org.jboss.dna.graph.ExecutionContextFactory;

/**
 * The context for a repository. This interface need not be implemented by a {@link RepositorySource}, as it is normally provided
 * to the source when {@link RepositorySource#initialize(RepositoryContext) initialized}.
 * 
 * @author Randall Hauch
 */
public interface RepositoryContext {

    /**
     * Get the factory that can be used to create execution contexts.
     * 
     * @return the execution context factory
     */
    ExecutionContextFactory getExecutionContextFactory();

    /**
     * Get the factory for {@link RepositoryConnection connections} to other sources.
     * 
     * @return the connection factory
     */
    RepositoryConnectionFactory getRepositoryConnectionFactory();

}
