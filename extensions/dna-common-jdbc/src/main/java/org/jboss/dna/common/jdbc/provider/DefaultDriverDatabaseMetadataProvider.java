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

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Default DatabaseMetadataProvider based on driver
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DefaultDriverDatabaseMetadataProvider extends DefaultDatabaseMetadataProvider
    implements DriverDatabaseMetadataProvider {
    // ~ Instance fields ------------------------------------------------------------------

    private static final long serialVersionUID = -3616979905696406464L;
    private String driverClassName;
    private String databaseUrl;
    private String userName;
    private String password;

    // ~ Constructors ---------------------------------------------------------------------

    /**
     * Default constructor
     */
    public DefaultDriverDatabaseMetadataProvider() {
    }

    /**
     * Constructor
     * 
     * @param name the DatabaseMetadataProvider logical name
     */
    public DefaultDriverDatabaseMetadataProvider( String name ) {
        super(name);
    }

    // ~ Methods --------------------------------------------------------------------------

    /**
     * Opens new database connection based on suppied parameters
     * 
     * @return new database connection based on suppied parameters
     * @throws Exception
     */
    @Override
    protected Connection openConnection() throws Exception {
        // log debug info
        if (log.isDebugEnabled()) {
            log.debug("Loading JDBC driver class: " + getDriverClassName());
        }

        // trying to load database driver by name
        Class.forName(getDriverClassName());

        // log debug info
        if (log.isDebugEnabled()) {
            log.debug("Opening database connection by using driver manager. The URL: " + getDatabaseUrl() + ". The user: "
                      + getUserName());
        }

        // opening connection by using Driver manager
        return DriverManager.getConnection(getDatabaseUrl(), getUserName(), getPassword());
    }

    /**
     * Gets JDBC driver class name
     * 
     * @return the JDBC driver class name
     */
    public String getDriverClassName() {
        // return
        return driverClassName;
    }

    /**
     * Sets JDBC driver class name
     * 
     * @param driverClassName the JDBC driver class name
     */
    public void setDriverClassName( String driverClassName ) {
        this.driverClassName = driverClassName;
    }

    /**
     * Gets database URL as string
     * 
     * @return database URL as string
     */
    public String getDatabaseUrl() {
        // return
        return databaseUrl;
    }

    /**
     * Sets the database URL as string
     * 
     * @param databaseUrl the database URL as string
     */
    public void setDatabaseUrl( String databaseUrl ) {
        this.databaseUrl = databaseUrl;
    }

    /**
     * Gets the user name
     * 
     * @return the user name
     */
    public String getUserName() {
        // return
        return userName;
    }

    /**
     * Sets the user name
     * 
     * @param userName the user name
     */
    public void setUserName( String userName ) {
        this.userName = userName;
    }

    /**
     * Get user's password
     * 
     * @return user's password
     */
    public String getPassword() {
        // return
        return password;
    }

    /**
     * Sets the user's password
     * 
     * @param password the user's password
     */
    public void setPassword( String password ) {
        this.password = password;
    }
}
