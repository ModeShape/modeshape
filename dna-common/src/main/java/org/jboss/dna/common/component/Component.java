/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.component;

/**
 * The interface for a DNA component, which sequences nodes and their content to extract additional information from the
 * information.
 * <p>
 * Implementations must provide a no-argument constructor.
 * </p>
 * @author Randall Hauch
 * @param <T> the type of configuration
 */
public interface Component<T extends ComponentConfig> {

    /**
     * This method allows the implementation to initialize and configure itself using the supplied {@link ComponentConfig}
     * information, and is called prior to any other class to this object. When this method is called, the implementation must
     * maintain a reference to the supplied configuration (which should then be returned in {@link #getConfiguration()}.
     * @param configuration the configuration for the component
     */
    void setConfiguration( T configuration );

    /**
     * Return the configuration for this component, as supplied to the last {@link #setConfiguration(ComponentConfig)} invocation.
     * @return the configuration, or null if not yet configured
     */
    T getConfiguration();

}
