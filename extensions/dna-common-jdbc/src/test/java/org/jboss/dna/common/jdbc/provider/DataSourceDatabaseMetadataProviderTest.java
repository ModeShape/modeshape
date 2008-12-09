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

import java.util.Properties;
import junit.framework.TestCase;

/**
 * Data Source Database Metadata Provider Test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DataSourceDatabaseMetadataProviderTest extends TestCase {
    // ~ Static fields/initializers -------------------------------------------------------

    private static final String PROVIDER_NAME = "TestProvider";
    private static final String DATA_SOURCE_NAME = "java:comp/env/jdbc/TestDataSource";
    private static final String INITIAL_CONTEXT_FACTORY = "org.jnp.interfaces.NamingContextFactory";
    private static final String PROVIDER_URL = "jnp://localhost:1099/";
    private final static String EMPTY_STRING_NOTATION = "<EMPTY>";
    private final static String NULL_STRING_NOTATION = "<NULL>";

    // ~ Instance fields ------------------------------------------------------------------

    private DataSourceDatabaseMetadataProvider dataSourceProvider;

    // ~ Methods --------------------------------------------------------------------------

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create provider
        dataSourceProvider = new DefaultDataSourceDatabaseMetadataProvider();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        // release
        dataSourceProvider = null;
        super.tearDown();
    }

    /**
     * Test get data source
     * 
     * @throws Exception
     */
    public void testGetDataSource() throws Exception {
        // set provider name
        dataSourceProvider.setName(PROVIDER_NAME);

        // set data source name
        dataSourceProvider.setDataSourceName(DATA_SOURCE_NAME);

        // create properties
        Properties properties = new Properties();

        // set initial context factory
        properties.put("java.naming.factory.initial", INITIAL_CONTEXT_FACTORY);

        // set provider URL
        properties.put("java.naming.provider.url", PROVIDER_URL);

        // trying to set properties
        dataSourceProvider.setProperties(properties);

        // TODO: validate the testGetDataSource
        try {
            // trying to get data source
            dataSourceProvider.getDataSource();
            fail("DataSource provider should raise an exception when app server is not running");
        } catch (Exception e) {
            // we're should be here because app server is not running so far
        }
    }

    /**
     * test set data source name
     */
    public void testSetDataSourceName() {
        // set data source name
        dataSourceProvider.setDataSourceName(DATA_SOURCE_NAME);

        // check that we really set the name
        assertEquals("Unable to set the data source name", DATA_SOURCE_NAME, dataSourceProvider.getDataSourceName());
    }

    /**
     * test release resources
     */
    public void testRelease() {
        dataSourceProvider.release(true);
    }

    /**
     * test get database meta data
     */
    public void testGetDatabaseMetaData() {
        // set provider name
        dataSourceProvider.setName(PROVIDER_NAME);

        // set data source name
        dataSourceProvider.setDataSourceName(DATA_SOURCE_NAME);

        // create properties
        Properties properties = new Properties();

        // set initial context factory
        properties.put("java.naming.factory.initial", INITIAL_CONTEXT_FACTORY);

        // set provider URL
        properties.put("java.naming.provider.url", PROVIDER_URL);

        // trying to set properties
        dataSourceProvider.setProperties(properties);

        // TODO: validate the testGetDatabaseMetaData
        try {
            // trying to get connection
            dataSourceProvider.getDatabaseMetaData();
            fail("DataSource provider should raise an exception when app server is not running");
        } catch (Exception e) {
            // we're should be here because app server is not running so far
        }
    }

    /**
     * Test get connection
     * 
     * @throws Exception
     */
    public void testGetConnection() throws Exception {
        // set provider name
        dataSourceProvider.setName(PROVIDER_NAME);

        // set data source name
        dataSourceProvider.setDataSourceName(DATA_SOURCE_NAME);

        // create properties
        Properties properties = new Properties();

        // set initial context factory
        properties.put("java.naming.factory.initial", INITIAL_CONTEXT_FACTORY);

        // set provider URL
        properties.put("java.naming.provider.url", PROVIDER_URL);

        // trying to set properties
        dataSourceProvider.setProperties(properties);

        // TODO: validate the testGetConnection
        try {
            // trying to get connection
            dataSourceProvider.getConnection();
            fail("DataSource provider should raise an exception when app server is not running");
        } catch (Exception e) {
            // we're should be here because app server is not running so far
        }
    }

    /**
     * Test set name
     */
    public void testSetName() {
        // set provider name
        dataSourceProvider.setName(PROVIDER_NAME);

        // check that we really set the name
        assertEquals("Unable to set the data source provider name", PROVIDER_NAME, dataSourceProvider.getName());
    }

    /**
     * Test set properties
     */
    public void testSetProperties() {
        // create properties
        Properties properties = new Properties();

        // set initial context factory
        properties.put("java.naming.factory.initial", INITIAL_CONTEXT_FACTORY);

        // set provider URL
        properties.put("java.naming.provider.url", PROVIDER_URL);

        // trying to set properties
        dataSourceProvider.setProperties(properties);

        // check that we're really set the properties
        assertSame("Unable to set the data source provider properties", properties, dataSourceProvider.getProperties());
    }

    /**
     * Test set string notation
     */
    public void testSetEmptyStringNotation() {
        // trying to set empty string notation
        dataSourceProvider.setEmptyStringNotation(EMPTY_STRING_NOTATION);

        // check that we're really set the empty string notation
        assertSame("Unable to set the empty string notation", EMPTY_STRING_NOTATION, dataSourceProvider.getEmptyStringNotation());
    }

    /**
     * Test set null string notation
     */
    public void testSetNullStringNotation() {
        // trying to set NULL string notation
        dataSourceProvider.setNullStringNotation(NULL_STRING_NOTATION);

        // check that we're really set the empty string notation
        assertSame("Unable to set the NULL string notation", NULL_STRING_NOTATION, dataSourceProvider.getNullStringNotation());
    }
}
