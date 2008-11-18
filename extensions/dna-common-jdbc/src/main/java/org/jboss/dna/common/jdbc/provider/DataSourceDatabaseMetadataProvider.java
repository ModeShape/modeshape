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

import javax.sql.DataSource;

/**
 * DataSource based Database Metadata Provider (such as JEE App container based)
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface DataSourceDatabaseMetadataProvider extends DatabaseMetadataProvider {
    // ~ Methods --------------------------------------------------------------------------

    /**
     * Returns DataSource
     * 
     * @return DataSource
     * @throws Exception 
     */
    DataSource getDataSource() throws Exception;

    /**
     * Gets data source JNDI name
     * 
     * @return data source JNDI name
     */
    String getDataSourceName();

    /**
     * Sets data source JNDI name
     * 
     * @param dataSourceName the data source JNDI name
     */
    void setDataSourceName( String dataSourceName );
}
