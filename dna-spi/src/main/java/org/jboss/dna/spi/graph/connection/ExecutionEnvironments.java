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

import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.impl.DelegatingValueFactories;

/**
 * Utility methods for creating various execution environments with replacement factories or components.
 * 
 * @author Randall Hauch
 */
public class ExecutionEnvironments {

    /**
     * Create an environment that can be used to replace the supplied environment but that uses the supplied path factory.
     * 
     * @param env the base environment
     * @param pathFactory the new path factory
     * @return the new execution environment
     * @throws IllegalArgumentException if the environment or factory references are null
     */
    public static ExecutionEnvironment replace( ExecutionEnvironment env,
                                                PathFactory pathFactory ) {
        ArgCheck.isNotNull(env, "env");
        ArgCheck.isNotNull(pathFactory, "pathFactory");
        return new DelegatingExecutionEnvironment(env, null, null, null, pathFactory);
    }

    /**
     * Create an environment that can be used to replace the supplied environment but that uses the supplied name factory.
     * 
     * @param env the base environment
     * @param nameFactory the new name factory
     * @return the new execution environment
     * @throws IllegalArgumentException if the environment or factory references are null
     */
    public static ExecutionEnvironment replace( ExecutionEnvironment env,
                                                NameFactory nameFactory ) {
        ArgCheck.isNotNull(env, "env");
        ArgCheck.isNotNull(nameFactory, "nameFactory");
        return new DelegatingExecutionEnvironment(env, null, null, nameFactory, null);
    }

    /**
     * Create an environment that can be used to replace the supplied environment but that uses the supplied name and path
     * factories.
     * 
     * @param env the base environment
     * @param nameFactory the new name factory
     * @param pathFactory the new path factory
     * @return the new execution environment
     * @throws IllegalArgumentException if the environment or factory references are null
     */
    public static ExecutionEnvironment replace( ExecutionEnvironment env,
                                                NameFactory nameFactory,
                                                PathFactory pathFactory ) {
        ArgCheck.isNotNull(env, "env");
        ArgCheck.isNotNull(nameFactory, "nameFactory");
        ArgCheck.isNotNull(pathFactory, "pathFactory");
        return new DelegatingExecutionEnvironment(env, null, null, nameFactory, pathFactory);
    }

    /**
     * Create an environment that can be used to replace the supplied environment but that uses the supplied namespace registry.
     * 
     * @param env the base environment
     * @param namespaceRegistry the new namespace registry
     * @return the new execution environment
     * @throws IllegalArgumentException if the environment or registry references are null
     */
    public static ExecutionEnvironment replace( ExecutionEnvironment env,
                                                NamespaceRegistry namespaceRegistry ) {
        ArgCheck.isNotNull(env, "env");
        ArgCheck.isNotNull(namespaceRegistry, "namespaceRegistry");
        return new DelegatingExecutionEnvironment(env, namespaceRegistry, null, null, null);
    }

    protected static class DelegatingExecutionEnvironment implements ExecutionEnvironment {

        private final ExecutionEnvironment delegate;
        private final NamespaceRegistry newNamespaceRegistry;
        private final PropertyFactory newPropertyFactory;
        private final ValueFactories newValueFactories;

        public DelegatingExecutionEnvironment( ExecutionEnvironment delegate,
                                               NamespaceRegistry newRegistry,
                                               PropertyFactory newPropertyFactory,
                                               ValueFactories newValueFactories ) {
            assert delegate != null;
            this.delegate = delegate;
            this.newNamespaceRegistry = newRegistry;
            this.newPropertyFactory = newPropertyFactory;
            this.newValueFactories = newValueFactories;
        }

        public DelegatingExecutionEnvironment( ExecutionEnvironment delegate,
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
         * @see org.jboss.dna.spi.graph.connection.ExecutionEnvironment#getNamespaceRegistry()
         */
        public NamespaceRegistry getNamespaceRegistry() {
            if (newNamespaceRegistry != null) return newNamespaceRegistry;
            return delegate.getNamespaceRegistry();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.ExecutionEnvironment#getPropertyFactory()
         */
        public PropertyFactory getPropertyFactory() {
            if (newPropertyFactory != null) return newPropertyFactory;
            return delegate.getPropertyFactory();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.ExecutionEnvironment#getValueFactories()
         */
        public ValueFactories getValueFactories() {
            if (newValueFactories != null) return newValueFactories;
            return delegate.getValueFactories();
        }

        /**
         * @return delegate
         */
        protected ExecutionEnvironment getDelegate() {
            return delegate;
        }

    }
}
