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
