/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jdbc;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import javax.jcr.Repository;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.jdbc.delegate.ConnectionInfo;
import org.modeshape.jdbc.delegate.RepositoryDelegate;

/**
 * 
 */
public class JcrConnectionTest {

    private static final String REPOSITORY_NAME = "repositoryName";

    private Connection conn;

    @Mock
    private ConnectionInfo connInfo;

    @Mock
    private Repository repository;

    @Mock
    private Session session;

    @Mock
    private RepositoryDelegate jcrDelegate;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        conn = new JcrConnection(jcrDelegate);

        // Set up the connection information ...
        when(connInfo.getWorkspaceName()).thenReturn("workspaceName");
        when(connInfo.getRepositoryName()).thenReturn(REPOSITORY_NAME);

        when(jcrDelegate.isValid(anyInt())).thenReturn(Boolean.TRUE);
        when(jcrDelegate.getConnectionInfo()).thenReturn(connInfo);

        when(repository.login(anyString())).thenReturn(session);
        when(repository.getDescriptor(anyString())).thenReturn("modeshape");

        when(session.getRepository()).thenReturn(repository);

    }

    @After
    public void afterEach() throws Exception {
        try {
            conn.close();
        } catch (Exception e) {

        }

    }

    @Test
    public void shouldCallClearWarnings() throws SQLException {
        conn.clearWarnings();
    }

    @Test
    public void shouldCallClose() throws SQLException {
        conn.close();
    }

    @Test
    public void shouldCallCommit() throws SQLException {
        conn.commit();
    }

    @Test
    public void shouldCallGetCatalog() throws SQLException {
        assertThat(conn.getCatalog(), is(REPOSITORY_NAME));
    }

    @Test
    public void shouldCallCreateStatement() throws SQLException {
        Statement stmt = conn.createStatement();
        assertThat(stmt, is(notNullValue()));
        assertThat(stmt, is(instanceOf(JcrStatement.class)));
    }

    @Test
    public void shouldCallGetAutoCommit() throws SQLException {
        assertThat(conn.getAutoCommit(), is(true));
    }

    @Test
    public void shouldCallGetClientInfo() throws SQLException {
        assertThat(conn.getClientInfo(), is(new Properties()));
    }

    @Test
    public void shouldCallGetClientInfoByName() throws SQLException {
        conn.setClientInfo("prop1", "prop1Value");
        matchClientInfoByName("prop1", "prop1Value");
    }

    private void matchClientInfoByName( String name,
                                        String match ) throws SQLException {
        assertThat(conn.getClientInfo(name), is(match));
    }

    @Test
    public void shouldCallGetMetaData() throws SQLException {
        assertNotNull(conn.getMetaData());
    }

    @Test
    public void shouldCallGetTransactionIsolation() throws SQLException {
        assertThat(conn.getTransactionIsolation(), is(Connection.TRANSACTION_READ_COMMITTED));
    }

    @Test
    public void shouldCallGetTypeMap() throws SQLException {
        assertEquals(conn.getTypeMap(), new HashMap<Object, Object>());
    }

    @Test
    public void ShouldCallGetWarnings() throws SQLException {
        assertNull(conn.getWarnings());
    }

    @Test
    public void shouldCallIsClosed() throws SQLException {
        assertThat(conn.isClosed(), is(false));
        conn.close();
        assertThat(conn.isClosed(), is(true));
    }

    @Test
    public void shouldCallIsReadOnly() throws SQLException {
        assertThat(conn.isReadOnly(), is(true));
    }

    @Test
    public void shouldCallIsValid() throws SQLException {
        assertThat(conn.isValid(0), is(true));
        assertThat(conn.isValid(120), is(true));
        assertThat(conn.isValid(1200), is(true));
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLException.class )
    public void shouldThrowExceptionWhenIsValidArgIsLessThanZero() throws SQLException {
        conn.isValid(-1);
    }

    @Test
    public void shouldCallNativeSQL() throws SQLException {
        conn.nativeSQL("sql");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallPrepareStatementAutoGenKeys() throws SQLException {
        conn.prepareStatement("sql", 1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallPrepareStatementColumnIndexes() throws SQLException {
        conn.prepareStatement("sql", new int[] {});
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallPrepareStatementColumnNames() throws SQLException {
        conn.prepareStatement("sql", new String[] {});
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingPrepareStatement() throws SQLException {
        conn.prepareStatement("sql");
    }

    @Test
    public void shouldCallRollback() throws SQLException {
        conn.rollback();
    }

    @Test
    public void shouldCallSetAutoCommit() throws SQLException {
        conn.setAutoCommit(false);
        assertThat(conn.getAutoCommit(), is(false));
    }

    @Test
    public void shouldCallSetClientInfoProperties() throws SQLException {
        Properties props = new Properties();
        props.setProperty("prop1", "prop1Value1");
        props.setProperty("prop2", "prop1Value2");

        conn.setClientInfo(props);
        assertThat(conn.getClientInfo(), is(props));
    }

    @Test
    public void shouldCallSetClientInfoPropertyValue() throws SQLException {
        conn.setClientInfo("propertyname", "propvalue");
        matchClientInfoByName("propertyname", "propvalue");
    }

    @Test
    public void shouldCallSetReadOnly() throws SQLException {
        conn.setReadOnly(true);
        // confirm that readonly mode is never changed
        assertThat(conn.isReadOnly(), is(true));
    }

    @Test
    public void shouldCallIsWrapperFor() throws SQLException {
        assertThat(conn.isWrapperFor(JcrConnection.class), is(true));
    }

    @Test
    public void shouldCallUnwrap() throws SQLException {
        assertThat(conn.unwrap(JcrConnection.class), is(instanceOf(JcrConnection.class)));
    }

    // *****************************
    // Unsupported Features
    // *****************************

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingSavepoint() throws SQLException {
        conn.rollback(null);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingSetSavepoint() throws SQLException {
        conn.setSavepoint();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingSetSavepointPassingArg() throws SQLException {
        conn.setSavepoint(null);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingGetHoldability() throws SQLException {
        conn.getHoldability();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingSetHoldability() throws SQLException {
        conn.setHoldability(1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingCreateStatement3Parms() throws SQLException {
        conn.createStatement(1, 1, 1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingCreateStatement2Parms() throws SQLException {
        conn.createStatement(1, 1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingPrepareCall3Parms() throws SQLException {
        conn.prepareCall("sql", 1, 1, 1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingPrepareCall2Parms() throws SQLException {
        conn.prepareCall("sql", 1, 1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingPrepareCall() throws SQLException {
        conn.prepareCall("sql");
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingPrepareStatementWith3Parms() throws SQLException {
        conn.prepareStatement("sql", 1, 1);
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingPrepareStatementWith4Parms() throws SQLException {
        conn.prepareStatement("sql", 1, 1, conn.getHoldability());
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingCreateArrayOf() throws SQLException {
        conn.createArrayOf("typename", new Object[] {});
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingCreateBlob() throws SQLException {
        conn.createBlob();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingCreateClob() throws SQLException {
        conn.createClob();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingCreateNClob() throws SQLException {
        conn.createNClob();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallinCreateSQLXML() throws SQLException {
        conn.createSQLXML();
    }

    /**
     * @throws SQLException
     */
    @Test( expected = SQLFeatureNotSupportedException.class )
    public void featureNotSupportedCallingCreateStruct() throws SQLException {
        conn.createStruct("typeName", new Object[] {});
    }

    /**
     * This feature is not supported, but it doesnt throw the SQLFeatureNotSupportedException, it silently ignores any passed in
     * argument.
     * 
     * @throws SQLException
     */
    @Test
    public void featureNotSupportedCallingSetTransactionIsolation() throws SQLException {
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
    }

    /**
     * This feature is not supported, but it doesnt throw the SQLFeatureNotSupportedException, it silently ignores any passed in
     * argument.
     * 
     * @throws SQLException
     */
    @Test
    public void featureNotSupportedCallingSetCatalog() throws SQLException {
        conn.setCatalog("catalog");
    }

}
