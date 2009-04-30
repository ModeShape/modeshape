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

import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.jdbc.JdbcMetadataI18n;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Properties;

/**
 * Default DatabaseMetadataProvider
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public abstract class DefaultDatabaseMetadataProvider implements DatabaseMetadataProvider {
    // ~ Instance fields ------------------------------------------------------------------
    private static final long serialVersionUID = -4164910060171439260L;
    private String name;
    private Properties properties;
    private String emptyStringNotation;
    private String nullStringNotation;

    /**
     * Logging for this instance
     */
    protected Logger log = Logger.getLogger(getClass());

    /**
     * Database metadata
     */
    protected DatabaseMetaData databaseMetaData;

    /**
     * Datbase connection
     */
    protected Connection connection;

    // ~ Constructors ---------------------------------------------------------------------

    /**
     * Default Constructor
     */
    public DefaultDatabaseMetadataProvider() {
    }

    /**
     * Constructor
     * 
     * @param name the DatabaseMetadataProvider logical name
     */
    public DefaultDatabaseMetadataProvider( String name ) {
        this();
        setName(name);
    }

    // ~ Methods --------------------------------------------------------------------------

    /**
     * Opens new database connection based on supplied parameters
     * 
     * @return new database connection based on supplied parameters
     * @throws Exception 
     */
    protected abstract Connection openConnection() throws Exception;

    /**
     * Releases database resources
     * 
     * @param silently if true never generates Exception; otherwise mage rethrow RunTimeException
     */
    public void release( boolean silently ) {
        // releases databaseMetaData
        if (databaseMetaData != null) {
            databaseMetaData = null;
        }

        // releases connection
        if (connection != null) {
            try {
                // close connection
                connection.close();

                log.debug (JdbcMetadataI18n.databaseConnectionHasBeenReleased.text(getName()));
            } catch (Exception ex) {
                log.error(JdbcMetadataI18n.errorClosingDatabaseConnection, ex);

                if (!silently) {
                    throw new RuntimeException(
                         JdbcMetadataI18n.errorClosingDatabaseConnection.text(getName()), ex);
                }
            }
        }
    }

    /**
     * Returns database metadata
     * 
     * @return database metadata
     * @throws Exception 
     */
    public DatabaseMetaData getDatabaseMetaData() throws Exception {
        // lazy load of database metadata
        if (databaseMetaData == null) {
            // log debug info
            if (log.isDebugEnabled()) {
                log.debug(String.format("Getting Database metadata for a provider %1$s", getName()));
            }

            // obtains metadata from connection
            databaseMetaData = getConnection().getMetaData();
            // log debug info
            if (log.isDebugEnabled()) {
                log.debug(String.format("Database metadata received for a provider %1$s", getName()));
            }
        }

        // return
        return databaseMetaData;
    }

    /**
     * Returns database connection
     * 
     * @return database connection
     * @throws Exception 
     */
    public Connection getConnection() throws Exception {
        // lazy open of connection
        if (connection == null) {
            // opens new connectection
            connection = openConnection();

            // log debug info
            log.info(JdbcMetadataI18n.databaseConnectionHasBeenEstablished, getName());
        }

        // return
        return connection;
    }

    /**
     * Returns DatabaseMetadataProvider logical name
     * 
     * @return the DatabaseMetadataProvider logical name
     */
    public String getName() {
        // return
        return name;
    }

    /**
     * Sets the DatabaseMetadataProvider logical name
     * 
     * @param name the DatabaseMetadataProvider logical name
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Get provider's notation for empty string
     * 
     * @return provider's notation for empty string
     */
    public String getEmptyStringNotation() {
        return emptyStringNotation;
    }

    /**
     * Set provider's notation for empty string
     * 
     * @param emptyStringNotation the provider's notation for empty string
     */
    public void setEmptyStringNotation( String emptyStringNotation ) {
        this.emptyStringNotation = emptyStringNotation;
    }

    /**
     * Get provider's notation for NULL string
     * 
     * @return provider's notation for NULL string
     */
    public String getNullStringNotation() {
        return nullStringNotation;
    }

    /**
     * Set provider's notation for NULL string
     * 
     * @param nullStringNotation the provider's notation for NULL string
     */
    public void setNullStringNotation( String nullStringNotation ) {
        this.nullStringNotation = nullStringNotation;
    }

    /**
     * Returns provider properties
     * 
     * @return provider properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the provider properties
     * 
     * @param properties the provider properties
     */
    public void setProperties( Properties properties ) {
        this.properties = properties;
    }
}
