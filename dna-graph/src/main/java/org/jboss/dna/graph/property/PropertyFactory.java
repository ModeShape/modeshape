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
package org.jboss.dna.graph.property;

import java.util.Iterator;

/**
 * @author Randall Hauch
 */
public interface PropertyFactory {
    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     Object... values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     Iterable<?> values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     Iterator<?> values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Object... values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Iterable<?> values );

    /**
     * Create a property with the supplied name and values
     * 
     * @param name the property name; may not be null
     * @param desiredType the type that the objects should be converted to; if null, they will be used as is
     * @param values the values
     * @return the resulting property
     */
    Property create( Name name,
                     PropertyType desiredType,
                     Iterator<?> values );

}
