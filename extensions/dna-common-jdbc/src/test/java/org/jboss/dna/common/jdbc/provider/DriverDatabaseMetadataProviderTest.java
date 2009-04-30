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

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Properties;

/**
 * Driver Database Metadata Provider Test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DriverDatabaseMetadataProviderTest extends TestCase {
    // ~ Static fields/initializers -------------------------------------------------------

    private static final String PROVIDER_NAME = "TestProvider";
    private final static String DRIVER_CLASS_NAME = "org.hsqldb.jdbcDriver";
    private final static String DATABASE_URL = "jdbc:hsqldb:mem:test";
    private final static String USER_NAME = "sa";
    private final static String PASSWORD = "";
    private final static String EMPTY_STRING_NOTATION = "<EMPTY>";
    private final static String NULL_STRING_NOTATION = "<NULL>";

    // ~ Instance fields ------------------------------------------------------------------

    private DriverDatabaseMetadataProvider driverProvider;

    // ~ Methods --------------------------------------------------------------------------

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // create provider
        driverProvider = new DefaultDriverDatabaseMetadataProvider();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        // release
        driverProvider = null;
        super.tearDown();
    }

    /**
     * COMMENT
     */
    public void testSetDriverClassName() {
        // set the driver class name
        driverProvider.setDriverClassName(DRIVER_CLASS_NAME);

        // check that we're really set the driver class name
        assertEquals("Unable to set the driver class name", DRIVER_CLASS_NAME, driverProvider.getDriverClassName());
    }

    /**
     * COMMENT
     */
    public void testSetDatabaseUrl() {
        // set the database URL
        driverProvider.setDatabaseUrl(DATABASE_URL);

        // check that we're really set the database URL
        assertEquals("Unable to set the database URL", DATABASE_URL, driverProvider.getDatabaseUrl());
    }

    /**
     * COMMENT
     */
    public void testSetUserName() {
        // set the user name
        driverProvider.setUserName(USER_NAME);

        // check that we're really set the user name
        assertEquals("Unable to set the user name", USER_NAME, driverProvider.getUserName());
    }

    /**
     * COMMENT
     */
    public void testSetPassword() {
        // set the password
        driverProvider.setPassword(PASSWORD);

        // check that we're really set the password
        assertEquals("Unable to set the password", PASSWORD, driverProvider.getPassword());
    }

    /**
     * COMMENT
     */
    public void testRelease() {
        driverProvider.release(true);
    }

    /**
     * COMMENT
     * 
     * @throws Exception COMMENT
     */
    public void testGetDatabaseMetaData() throws Exception {
        // set provider name
        driverProvider.setName(PROVIDER_NAME);

        // set the driver class name
        driverProvider.setDriverClassName(DRIVER_CLASS_NAME);

        // set the database URL
        driverProvider.setDatabaseUrl(DATABASE_URL);

        // set the user name
        driverProvider.setUserName(USER_NAME);

        // set the password
        driverProvider.setPassword(PASSWORD);

        // validate the testGetDatabaseMetaData
        try {
            // trying to get connection
            DatabaseMetaData databaseMetaData = driverProvider.getDatabaseMetaData();
            // check
            assertNotNull("Database metadata shall be provided", databaseMetaData);
            // fail ("Driver provider should raise an exception when DB server is not running");
            // } catch (Exception e) {
            // we're should be here because DB server is not running so far
            // }
        } finally {
            // release resource
            driverProvider.release(true);
        }
    }

    /**
     * COMMENT
     * 
     * @throws Exception COMMENT
     */
    public void testGetConnection() throws Exception {
        // set provider name
        driverProvider.setName(PROVIDER_NAME);

        // set the driver class name
        driverProvider.setDriverClassName(DRIVER_CLASS_NAME);

        // set the database URL
        driverProvider.setDatabaseUrl(DATABASE_URL);

        // set the user name
        driverProvider.setUserName(USER_NAME);

        // set the password
        driverProvider.setPassword(PASSWORD);

        // validate the testGetConnection
        try {
            // trying to get connection
            Connection connection = driverProvider.getConnection();
            assertNotNull("Database connection shall be provided", connection);    
            // fail ("Driver provider should raise an exception when DB server is not running");
            // } catch (Exception e) {
            // we're should be here because DB server is not running so far
            // }
        } finally {
            // release resource
            driverProvider.release(true);
        }
    }

    /**
     * COMMENT
     */
    public void testSetName() {
        // set provider name
        driverProvider.setName(PROVIDER_NAME);

        // check that we really set the name
        assertEquals("Unable to set the driver provider name", PROVIDER_NAME, driverProvider.getName());
    }

    /**
     * COMMENT
     */
    public void testSetProperties() {
        // create properties
        Properties properties = new Properties();

        // trying to set properties
        driverProvider.setProperties(properties);

        // check that we're really set the properties
        assertSame("Unable to set the driver provider properties", properties, driverProvider.getProperties());
    }

    /**
     * COMMENT
     */
    public void testSetEmptyStringNotation() {
        // trying to set empty string notation
        driverProvider.setEmptyStringNotation(EMPTY_STRING_NOTATION);

        // check that we're really set the empty string notation
        assertSame("Unable to set the empty string notation", EMPTY_STRING_NOTATION, driverProvider.getEmptyStringNotation());
    }

    /**
     * COMMENT
     */
    public void testSetNullStringNotation() {
        // trying to set NULL string notation
        driverProvider.setNullStringNotation(NULL_STRING_NOTATION);

        // check that we're really set the empty string notation
        assertSame("Unable to set the NULL string notation", NULL_STRING_NOTATION, driverProvider.getNullStringNotation());
    }
}
