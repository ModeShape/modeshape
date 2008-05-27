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
package org.jboss.dna.spi.connector;

import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.ValueFactories;

/**
 * @author Randall Hauch
 */
public interface RepositorySourceEnvironment {

    /**
     * Get the factories that should be used to create values for {@link Property properties}.
     * @return the property value factory; never null
     */
    ValueFactories getValueFactories();

    /**
     * Get the factory that should be used to create {@link Name names}.
     * @return the name factory; never null
     */
    NameFactory getNameFactory();

    /**
     * Get the {@link RepositorySource} instance that this environment represents. This instance will be the same instance that
     * created the {@link RepositoryConnection} for which this environment was created and is being used.
     * @return the repository source; never null
     */
    RepositorySource getRepositorySource();

}
