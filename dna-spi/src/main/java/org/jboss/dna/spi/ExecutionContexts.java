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

import java.security.AccessControlContext;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.impl.DelegatingValueFactories;

/**
 * Utility methods for creating various execution contexts with replacement factories or components.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class ExecutionContexts {

    /**
     * Create an context that can be used to replace the supplied context but that uses the supplied path factory.
     * 
     * @param context the base context
     * @param pathFactory the new path factory
     * @return the new execution context
     * @throws IllegalArgumentException if the context or factory references are null
     */
    public static ExecutionContext replace( ExecutionContext context,
                                            PathFactory pathFactory ) {
        ArgCheck.isNotNull(context, "context");
        ArgCheck.isNotNull(pathFactory, "pathFactory");
        return new DelegatingExecutionEnvironment(context, null, null, null, pathFactory);
    }

    /**
     * Create an context that can be used to replace the supplied context but that uses the supplied name factory.
     * 
     * @param context the base context
     * @param nameFactory the new name factory
     * @return the new execution context
     * @throws IllegalArgumentException if the context or factory references are null
     */
    public static ExecutionContext replace( ExecutionContext context,
                                            NameFactory nameFactory ) {
        ArgCheck.isNotNull(context, "context");
        ArgCheck.isNotNull(nameFactory, "nameFactory");
        return new DelegatingExecutionEnvironment(context, null, null, nameFactory, null);
    }

    /**
     * Create an context that can be used to replace the supplied context but that uses the supplied name and path factories.
     * 
     * @param context the base context
     * @param nameFactory the new name factory
     * @param pathFactory the new path factory
     * @return the new execution context
     * @throws IllegalArgumentException if the context or factory references are null
     */
    public static ExecutionContext replace( ExecutionContext context,
                                            NameFactory nameFactory,
                                            PathFactory pathFactory ) {
        ArgCheck.isNotNull(context, "context");
        ArgCheck.isNotNull(nameFactory, "nameFactory");
        ArgCheck.isNotNull(pathFactory, "pathFactory");
        return new DelegatingExecutionEnvironment(context, null, null, nameFactory, pathFactory);
    }

    /**
     * Create an context that can be used to replace the supplied context but that uses the supplied namespace registry.
     * 
     * @param context the base context
     * @param namespaceRegistry the new namespace registry
     * @return the new execution context
     * @throws IllegalArgumentException if the context or registry references are null
     */
    public static ExecutionContext replace( ExecutionContext context,
                                            NamespaceRegistry namespaceRegistry ) {
        ArgCheck.isNotNull(context, "context");
        ArgCheck.isNotNull(namespaceRegistry, "namespaceRegistry");
        return new DelegatingExecutionEnvironment(context, namespaceRegistry, null, null, null);
    }

    protected static class DelegatingExecutionEnvironment implements ExecutionContext {

        private final ExecutionContext delegate;
        private final NamespaceRegistry newNamespaceRegistry;
        private final PropertyFactory newPropertyFactory;
        private final ValueFactories newValueFactories;

        public DelegatingExecutionEnvironment( ExecutionContext delegate,
                                               NamespaceRegistry newRegistry,
                                               PropertyFactory newPropertyFactory,
                                               ValueFactories newValueFactories ) {
            assert delegate != null;
            this.delegate = delegate;
            this.newNamespaceRegistry = newRegistry;
            this.newPropertyFactory = newPropertyFactory;
            this.newValueFactories = newValueFactories;
        }

        public DelegatingExecutionEnvironment( ExecutionContext delegate,
                                               NamespaceRegistry newRegistry,
                                               PropertyFactory newPropertyFactory,
                                               final NameFactory newNameFactory,
                                               final PathFactory newPathFactory ) {
            assert delegate != null;
            this.delegate = delegate;
            this.newNamespaceRegistry = newRegistry;
            this.newPropertyFactory = newPropertyFactory;
            final PathFactory pathFactory = newPathFactory != null ? newPathFactory : delegate.getValueFactories().getPathFactory();
            final NameFactory nameFactory = newNameFactory != null ? newNameFactory : delegate.getValueFactories().getNameFactory();
            this.newValueFactories = newPathFactory == null ? null : new DelegatingValueFactories(delegate.getValueFactories()) {

                @Override
                public PathFactory getPathFactory() {
                    return pathFactory;
                }

                @Override
                public NameFactory getNameFactory() {
                    return nameFactory;
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.ExecutionContext#getAccessControlContext()
         */
        public AccessControlContext getAccessControlContext() {
            return delegate.getAccessControlContext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.ExecutionContext#getLoginContext()
         */
        public LoginContext getLoginContext() {
            return delegate.getLoginContext();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.ExecutionContext#getNamespaceRegistry()
         */
        public NamespaceRegistry getNamespaceRegistry() {
            if (newNamespaceRegistry != null) return newNamespaceRegistry;
            return delegate.getNamespaceRegistry();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.ExecutionContext#getPropertyFactory()
         */
        public PropertyFactory getPropertyFactory() {
            if (newPropertyFactory != null) return newPropertyFactory;
            return delegate.getPropertyFactory();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.ExecutionContext#getValueFactories()
         */
        public ValueFactories getValueFactories() {
            if (newValueFactories != null) return newValueFactories;
            return delegate.getValueFactories();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.ExecutionContext#getSubject()
         */
        public Subject getSubject() {
            return delegate.getSubject();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.ExecutionContext#getLogger(java.lang.Class)
         */
        public Logger getLogger( Class<?> clazz ) {
            return delegate.getLogger(clazz);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.ExecutionContext#getLogger(java.lang.String)
         */
        public Logger getLogger( String name ) {
            return delegate.getLogger(name);
        }

        /**
         * @return delegate
         */
        protected ExecutionContext getDelegate() {
            return delegate;
        }
    }
}
