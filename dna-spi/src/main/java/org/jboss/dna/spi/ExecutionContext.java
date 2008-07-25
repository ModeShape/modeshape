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
package org.jboss.dna.spi;

import javax.security.auth.Subject;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.ValueFactories;

/**
 * @author Randall Hauch
 */
public interface ExecutionContext {

    /**
     * Get the factories that should be used to create values for {@link Property properties}.
     * 
     * @return the property value factory; never null
     */
    ValueFactories getValueFactories();

    /**
     * Get the namespace registry for this context.
     * 
     * @return the namespace registry; never null
     */
    NamespaceRegistry getNamespaceRegistry();

    /**
     * Get the factory for creating {@link Property} objects.
     * 
     * @return the property factory; never null
     */
    PropertyFactory getPropertyFactory();

    /**
     * Get the JAAS subject for which this context was created.
     * 
     * @return the subject; never null
     */
    Subject getSubject();

}
