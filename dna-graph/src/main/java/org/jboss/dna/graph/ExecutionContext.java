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
package org.jboss.dna.graph;

import java.security.AccessControlContext;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.mimetype.MimeTypeDetector;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.ValueFactories;

/**
 * An ExecutionContext is a representation of the environment or context in which a component or operation is operating. Some
 * components require this context to be passed into individual methods, allowing the context to vary with each method invocation.
 * Other components require the context to be provided before it's used, and will use that context for all its operations (until
 * it is given a different one).
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 * @see ExecutionContextFactory
 */
public interface ExecutionContext extends ClassLoaderFactory {

    /**
     * @return the access control context; may be <code>null</code>
     */
    AccessControlContext getAccessControlContext();

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

    /**
     * Return an object that can be used to determine the MIME type of some content, such as the content of a file.
     * 
     * @return the detector; never null
     */
    MimeTypeDetector getMimeTypeDetector();

    /**
     * @return the login context; may be <code>null</code>
     */
    LoginContext getLoginContext();

    /**
     * Get the namespace registry for this context.
     * 
     * @return the namespace registry; never <code>null</code>
     */
    NamespaceRegistry getNamespaceRegistry();

    /**
     * Get the factory for creating {@link Property} objects.
     * 
     * @return the property factory; never <code>null</code>
     */
    PropertyFactory getPropertyFactory();

    /**
     * Get the JAAS subject for which this context was created.
     * 
     * @return the subject; never <code>null</code>
     */
    Subject getSubject();

    /**
     * Get the factories that should be used to create values for {@link Property properties}.
     * 
     * @return the property value factory; never null
     */
    ValueFactories getValueFactories();
}
