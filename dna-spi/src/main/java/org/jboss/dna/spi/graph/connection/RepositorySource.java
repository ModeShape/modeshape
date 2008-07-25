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
package org.jboss.dna.spi.graph.connection;

import java.io.Serializable;
import javax.naming.Referenceable;

/**
 * A repository source is a description of a resource that can be used to access or store repository information. This class
 * serves as a factory for {@link RepositoryConnection} instances and provides some basic configuration information.
 * <p>
 * Typically this interface is implemented by classes that provide standard-style getters and setters for the various properties
 * necessary for proper configuration via reflection or introspection. This interface expects nor defines any such properties,
 * leaving that entirely to the implementation classes.
 * </p>
 * <p>
 * Implementations should also provide a no-arg constructor so that it is possible to easily create instances and initialize using
 * the standard getters and setters. One example where this is required is when a RepositorySource instance is recorded in a
 * repository (e.g., in a configuration area), and needs to be reinstantiated.
 * </p>
 * <p>
 * Objects that implement this <code>RepositorySource</code> interface are typically registered with a naming service such as Java
 * Naming and Directory Interface<sup><font size=-3>TM</font></sup> (JNDI). This interface extends both {@link Referenceable} and
 * {@link Serializable} so that such objects can be stored in any JNDI naming context and enable proper system recovery,
 * </p>
 * 
 * @author Randall Hauch
 */
public interface RepositorySource extends ManagedRepositoryConnectionFactory, Referenceable, Serializable {

    /**
     * Get the maximum number of retries that may be performed on a given operation when using {@link #getConnection()
     * connections} created by this source. This value does not constitute a minimum number of retries; in fact, the connection
     * user is not required to retry any operations.
     * 
     * @return the maximum number of allowable retries, or 0 if the source has no limit
     */
    int getRetryLimit();

    /**
     * Set the maximum number of retries that may be performed on a given operation when using {@link #getConnection()
     * connections} created by this source. This value does not constitute a minimum number of retries; in fact, the connection
     * user is not required to retry any operations.
     * 
     * @param limit the maximum number of allowable retries, or 0 if the source has no limit
     */
    void setRetryLimit( int limit );

}
