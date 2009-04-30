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
package org.jboss.dna.common.jdbc.provider;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Properties;

/**
 * Database Meta data provider
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface DatabaseMetadataProvider extends Serializable {
    // ~ Methods --------------------------------------------------------------------------

    /**
     * Releases database resources
     * 
     * @param silently if true never generates Exception; otherwise mage rethrow RunTimeException
     */
    void release( boolean silently );

    /**
     * Returns database metadata
     * 
     * @return database metadata
     * @throws Exception 
     */
    DatabaseMetaData getDatabaseMetaData() throws Exception;

    /**
     * Returns database connection
     * 
     * @return database connection
     * @throws Exception 
     */
    Connection getConnection() throws Exception;

    /**
     * Returns DatabaseMetadataProvider logical name
     * 
     * @return the DatabaseMetadataProvider logical name
     */
    String getName();

    /**
     * Sets the DatabaseMetadataProvider logical name
     * 
     * @param name the DatabaseMetadataProvider logical name
     */
    void setName( String name );

    /**
     * Get provider's notation for empty string
     * 
     * @return provider's notation for empty string
     */
    String getEmptyStringNotation();

    /**
     * Set provider's notation for empty string
     * 
     * @param emptyStringNotation the provider's notation for empty string
     */
    void setEmptyStringNotation( String emptyStringNotation );

    /**
     * Get provider's notation for NULL string
     * 
     * @return provider's notation for NULL string
     */
    String getNullStringNotation();

    /**
     * Set provider's notation for NULL string
     * 
     * @param nullStringNotation the provider's notation for NULL string
     */
    void setNullStringNotation( String nullStringNotation );

    /**
     * Returns provider properties
     * 
     * @return provider properties
     */
    Properties getProperties();

    /**
     * Sets the provider properties
     * 
     * @param properties the provider properties
     */
    void setProperties( Properties properties );
}
