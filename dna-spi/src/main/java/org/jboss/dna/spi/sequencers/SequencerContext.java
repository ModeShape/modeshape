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
package org.jboss.dna.spi.sequencers;

import java.util.Set;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.ValueFactories;

/**
 * @author John Verhaeg
 */
public interface SequencerContext {

    /**
     * Get the factories that can be used to create {@link Path paths} and other property values.
     * 
     * @return the collection of factories; never <code>null</code>.
     */
    ValueFactories getFactories();

    /**
     * Return the path of the input node containing the content being sequenced.
     * 
     * @return input node's path.
     */
    Path getInputPath();

    /**
     * Return the set of properties from the input node containing the content being sequenced.
     * 
     * @return the input node's properties; never <code>null</code>.
     */
    Set<Property> getInputProperties();

    /**
     * Return the property with the supplied name from the input node containing the content being sequenced.
     * 
     * @param name
     * @return the input node property, or <code>null</code> if none exists.
     */
    Property getInputProperty( Name name );

    /**
     * Return the MIME-type of the content being sequenced.
     * 
     * @return the MIME-type
     */
    String getMimeType();

    /**
     * Convenience method to get the namespace registry used by the {@link ValueFactories#getNameFactory() name value factory}.
     * 
     * @return the namespace registry; never <code>null</code>.
     */
    NamespaceRegistry getNamespaceRegistry();

    /**
     * Return a logger associated with this context. This logger records only those activities within the context and provide a
     * way to capture the context-specific activities. All log messages are also sent to the system logger, so classes that log
     * via this mechanism should <i>not</i> also {@link Logger#getLogger(Class) obtain a system logger}.
     * 
     * @param clazz the class that is doing the logging
     * @return the logger, named after <code>clazz</code>; never null
     * @see #getLogger(String)
     */
    Logger getLogger( Class<?> clazz );

    /**
     * Return a logger associated with this context. This logger records only those activities within the context and provide a
     * way to capture the context-specific activities. All log messages are also sent to the system logger, so classes that log
     * via this mechanism should <i>not</i> also {@link Logger#getLogger(Class) obtain a system logger}.
     * 
     * @param name the name for the logger
     * @return the logger, named after <code>clazz</code>; never null
     * @see #getLogger(Class)
     */
    Logger getLogger( String name );

}
