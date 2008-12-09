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
package org.jboss.dna.common.jdbc.model.api;

import java.util.Map;

/**
 * Provides database named object specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface DatabaseNamedObject extends CoreMetaData {

    /**
     * Gets database named object name
     * 
     * @return database named object name
     */
    String getName();

    /**
     * Sets database named object name
     * 
     * @param name the database named object name
     */
    void setName( String name );

    /**
     * Gets explanatory comment on the database named object
     * 
     * @return explanatory comment on the database named object
     */
    String getRemarks();

    /**
     * Sets explanatory comment on the database named object
     * 
     * @param remarks the explanatory comment on the database named object
     */
    void setRemarks( String remarks );

    /**
     * Gets extra (non standard) properties if provided by database.
     * 
     * @return extra properties if provided by database
     */
    Map<String, Object> getExtraProperties();

    /**
     * Gets extra (non standard) property if provided by database.
     * 
     * @param key the key
     * @return extra property if provided by database
     */
    Object getExtraProperty( String key );

    /**
     * Adds extra property
     * 
     * @param key the key
     * @param value the value
     */
    void addExtraProperty( String key,
                           Object value );

    /**
     * deletes extra property
     * 
     * @param key the key
     */
    void deleteExtraProperty( String key );
}
