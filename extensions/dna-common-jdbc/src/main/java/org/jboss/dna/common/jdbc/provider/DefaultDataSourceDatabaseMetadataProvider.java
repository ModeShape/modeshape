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
package org.jboss.dna.common.jdbc.provider;

import java.sql.Connection;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Default DataSource based DatabaseMetadataProvider
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DefaultDataSourceDatabaseMetadataProvider extends DefaultDatabaseMetadataProvider
    implements DataSourceDatabaseMetadataProvider {
    // ~ Instance fields ------------------------------------------------------------------

    private static final long serialVersionUID = -405855619877872933L;
    private String dataSourceName;
    private DataSource dataSource;

    // ~ Constructors ---------------------------------------------------------------------

    /**
     * Default Constructor
     */
    public DefaultDataSourceDatabaseMetadataProvider() {
    }

    /**
     * Constructor
     * 
     * @param name the DatabaseMetadataProvider logical name
     */
    public DefaultDataSourceDatabaseMetadataProvider( String name ) {
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
            log.debug("Opening database connection from data source " + getDataSourceName());
        }

        // loads connection from data source
        return getDataSource().getConnection();
    }

    /**
     * Returns DataSource
     * 
     * @return DataSource
     * @throws Exception 
     */
    public DataSource getDataSource() throws Exception {
        // lazy load data source
        if (dataSource == null) {
            // log debug info
            if (log.isDebugEnabled()) {
                log.debug("Creating JNDI initial context with following properties: " + getProperties());
            }

            // create initial context using properties if any
            InitialContext initialContext = new InitialContext(getProperties());

            try {
                // log debug info
                if (log.isDebugEnabled()) {
                    log.debug("Performing JNDI lookup for data source " + getDataSourceName());
                }

                // lookups datasource
                dataSource = (DataSource)initialContext.lookup(getDataSourceName());
            } finally {
                // close context
                initialContext.close();
            }
        }

        // return
        return dataSource;
    }

    /**
     * Sets data source JNDI name
     * 
     * @return data source JNDI name
     */
    public String getDataSourceName() {
        // return
        return dataSourceName;
    }

    /**
     * Sets data source JNDI name
     * 
     * @param dataSourceName the data source JNDI name
     */
    public void setDataSourceName( String dataSourceName ) {
        this.dataSourceName = dataSourceName;
    }
}
