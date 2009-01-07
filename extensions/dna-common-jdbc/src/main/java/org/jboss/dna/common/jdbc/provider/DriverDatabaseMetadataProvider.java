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
package org.jboss.dna.common.jdbc.provider;

/**
 * DatabaseMetadataProvider based on simple driver/connection
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface DriverDatabaseMetadataProvider extends DatabaseMetadataProvider {
    // ~ Methods --------------------------------------------------------------------------

    /**
     * Gets JDBC driver class name
     * 
     * @return the JDBC driver class name
     */
    String getDriverClassName();

    /**
     * Sets JDBC driver class name
     * 
     * @param driverClassName the JDBC driver class name
     */
    void setDriverClassName( String driverClassName );

    /**
     * Gets database URL as string
     * 
     * @return database URL as string
     */
    String getDatabaseUrl();

    /**
     * Sets the database URL as string
     * 
     * @param databaseUrl the database URL as string
     */
    void setDatabaseUrl( String databaseUrl );

    /**
     * Gets the user name
     * 
     * @return the user name
     */
    String getUserName();

    /**
     * Sets the user name
     * 
     * @param userName the user name
     */
    void setUserName( String userName );

    /**
     * Get user's password
     * 
     * @return user's password
     */
    String getPassword();

    /**
     * Sets the user's password
     * 
     * @param password the user's password
     */
    void setPassword( String password );
}
