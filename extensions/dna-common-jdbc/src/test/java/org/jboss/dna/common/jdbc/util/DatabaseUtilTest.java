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
package org.jboss.dna.common.jdbc.util;

import junit.framework.TestCase;
import java.sql.Types;
import org.jboss.dna.common.jdbc.model.api.*;

/**
 * DatabaseUtil test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DatabaseUtilTest extends TestCase {

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /*
     * Class under test for Boolean getBoolean(ResultSet, String)
     */
    public void testGetBoolean() throws Exception {
        // check
        assertNull("getBoolean(null, 0) should return null", DatabaseUtil.getBoolean(null, 0));
        // check
        assertNull("getBoolean(null, 'myColumn') should return null", DatabaseUtil.getBoolean(null, "myColumn"));
    }

    /*
     * Class under test for Integer getInteger(ResultSet, String)
     */
    public void testGetInteger() throws Exception {
        // check
        assertNull("getInteger(null, 0) should return null", DatabaseUtil.getInteger(null, 0));
        // check
        assertNull("getInteger(null, 'myColumn') should return null", DatabaseUtil.getInteger(null, "myColumn"));
    }

    /*
     * Class under test for Double getDouble(ResultSet, String)
     */
    public void testGetDouble() throws Exception {
        // check
        assertNull("getDouble(null, 0) should return null", DatabaseUtil.getDouble(null, 0));
        // check
        assertNull("getDouble(null, 'myColumn') should return null", DatabaseUtil.getDouble(null, "myColumn"));
    }

    /*
     * Class under test for String getString(ResultSet, String)
     */
    public void testGetString() throws Exception {
        // check
        assertNull("getString(null, 0) should return null", DatabaseUtil.getString(null, 0));
        // check
        assertNull("getString(null, 'myColumn') should return null", DatabaseUtil.getString(null, "myColumn"));
    }

    public void testGetBestRowIdentifierScopeType() {
        // check
        assertNull("getBestRowIdentifierScopeType (null) should return null ", DatabaseUtil.getBestRowIdentifierScopeType(null));
        // check
        assertSame("getBestRowIdentifierScopeType () should return BestRowIdentifierScopeType.TEMPORARY",
                   BestRowIdentifierScopeType.TEMPORARY,
                   DatabaseUtil.getBestRowIdentifierScopeType(BestRowIdentifierScopeType.TEMPORARY.getScope()));
        // check
        assertSame("getBestRowIdentifierScopeType () should return BestRowIdentifierScopeType.TRANSACTION",
                   BestRowIdentifierScopeType.TRANSACTION,
                   DatabaseUtil.getBestRowIdentifierScopeType(BestRowIdentifierScopeType.TRANSACTION.getScope()));
        // check
        assertSame("getBestRowIdentifierScopeType () should return BestRowIdentifierScopeType.SESSION",
                   BestRowIdentifierScopeType.SESSION,
                   DatabaseUtil.getBestRowIdentifierScopeType(BestRowIdentifierScopeType.SESSION.getScope()));
    }

    public void testGetColumnPseudoType() {
        // check
        assertNull("getColumnPseudoType (null) should return null ", DatabaseUtil.getColumnPseudoType(null));
        // check
        assertSame("getColumnPseudoType () should return ColumnPseudoType.UNKNOWN",
                   ColumnPseudoType.UNKNOWN,
                   DatabaseUtil.getColumnPseudoType(ColumnPseudoType.UNKNOWN.getType()));
        // check
        assertSame("getColumnPseudoType () should return ColumnPseudoType.NOT_PSEUDO",
                   ColumnPseudoType.NOT_PSEUDO,
                   DatabaseUtil.getColumnPseudoType(ColumnPseudoType.NOT_PSEUDO.getType()));
        // check
        assertSame("getColumnPseudoType () should return ColumnPseudoType.PSEUDO",
                   ColumnPseudoType.PSEUDO,
                   DatabaseUtil.getColumnPseudoType(ColumnPseudoType.PSEUDO.getType()));
    }

    public void testGetIndexType() {
        // check
        assertNull("getIndexType (null) should return null ", DatabaseUtil.getIndexType(null));
        // check
        assertSame("getIndexType () should return IndexType.STATISTIC",
                   IndexType.STATISTIC,
                   DatabaseUtil.getIndexType(IndexType.STATISTIC.getType()));
        // check
        assertSame("getIndexType () should return IndexType.CLUSTERED",
                   IndexType.CLUSTERED,
                   DatabaseUtil.getIndexType(IndexType.CLUSTERED.getType()));
        // check
        assertSame("getIndexType () should return IndexType.HASHED",
                   IndexType.HASHED,
                   DatabaseUtil.getIndexType(IndexType.HASHED.getType()));
        // check
        assertSame("getIndexType () should return IndexType.OTHER",
                   IndexType.OTHER,
                   DatabaseUtil.getIndexType(IndexType.OTHER.getType()));
    }

    public void testGetKeyDeferrabilityType() {
        // check
        assertNull("getKeyDeferrabilityType (null) should return null ", DatabaseUtil.getKeyDeferrabilityType(null));
        // check
        assertSame("getKeyDeferrabilityType () should return KeyDeferrabilityType.INITIALLY_DEFERRED",
                   KeyDeferrabilityType.INITIALLY_DEFERRED,
                   DatabaseUtil.getKeyDeferrabilityType(KeyDeferrabilityType.INITIALLY_DEFERRED.getDeferrability()));
        // check
        assertSame("getKeyDeferrabilityType () should return KeyDeferrabilityType.INTIALLY_IMMEDIATE",
                   KeyDeferrabilityType.INTIALLY_IMMEDIATE,
                   DatabaseUtil.getKeyDeferrabilityType(KeyDeferrabilityType.INTIALLY_IMMEDIATE.getDeferrability()));
        // check
        assertSame("getKeyDeferrabilityType () should return KeyDeferrabilityType.NOT_DEFERRABLE",
                   KeyDeferrabilityType.NOT_DEFERRABLE,
                   DatabaseUtil.getKeyDeferrabilityType(KeyDeferrabilityType.NOT_DEFERRABLE.getDeferrability()));
    }

    public void testGetKeyModifyRuleType() {
        // check
        assertNull("getKeyModifyRuleType (null) should return null ", DatabaseUtil.getKeyModifyRuleType(null));
        // check
        assertSame("getKeyModifyRuleType () should return KeyModifyRuleType.CASCADE",
                   KeyModifyRuleType.CASCADE,
                   DatabaseUtil.getKeyModifyRuleType(KeyModifyRuleType.CASCADE.getRule()));
        // check
        assertSame("getKeyModifyRuleType () should return KeyModifyRuleType.RESTRICT",
                   KeyModifyRuleType.RESTRICT,
                   DatabaseUtil.getKeyModifyRuleType(KeyModifyRuleType.RESTRICT.getRule()));
        // check
        assertSame("getKeyModifyRuleType () should return KeyModifyRuleType.SET_NULL",
                   KeyModifyRuleType.SET_NULL,
                   DatabaseUtil.getKeyModifyRuleType(KeyModifyRuleType.SET_NULL.getRule()));
        // check
        assertSame("getKeyModifyRuleType () should return KeyModifyRuleType.NO_ACTION",
                   KeyModifyRuleType.NO_ACTION,
                   DatabaseUtil.getKeyModifyRuleType(KeyModifyRuleType.NO_ACTION.getRule()));
        // check
        assertSame("getKeyModifyRuleType () should return KeyModifyRuleType.SET_DEFAULT",
                   KeyModifyRuleType.SET_DEFAULT,
                   DatabaseUtil.getKeyModifyRuleType(KeyModifyRuleType.SET_DEFAULT.getRule()));
    }

    public void testGetNullabilityType() {
        // check
        assertNull("getNullabilityType (null) should return null ", DatabaseUtil.getNullabilityType(null));
        // check
        assertSame("getNullabilityType () should return NullabilityType.UNKNOWN",
                   NullabilityType.UNKNOWN,
                   DatabaseUtil.getNullabilityType(NullabilityType.UNKNOWN.getNullability()));
        // check
        assertSame("getNullabilityType () should return NullabilityType.NO_NULLS",
                   NullabilityType.NO_NULLS,
                   DatabaseUtil.getNullabilityType(NullabilityType.NO_NULLS.getNullability()));
        // check
        assertSame("getNullabilityType () should return NullabilityType.NULLABLE",
                   NullabilityType.NULLABLE,
                   DatabaseUtil.getNullabilityType(NullabilityType.NULLABLE.getNullability()));
    }

    public void testGetParameterIoType() {
        // check
        assertNull("getParameterIoType (null) should return null ", DatabaseUtil.getParameterIoType(null));
        // check
        assertSame("getParameterIoType () should return ParameterIoType.UNKNOWN",
                   ParameterIoType.UNKNOWN,
                   DatabaseUtil.getParameterIoType(ParameterIoType.UNKNOWN.getType()));
        // check
        assertSame("getParameterIoType () should return ParameterIoType.IN",
                   ParameterIoType.IN,
                   DatabaseUtil.getParameterIoType(ParameterIoType.IN.getType()));
        // check
        assertSame("getParameterIoType () should return ParameterIoType.IN_OUT",
                   ParameterIoType.IN_OUT,
                   DatabaseUtil.getParameterIoType(ParameterIoType.IN_OUT.getType()));
        // check
        assertSame("getParameterIoType () should return ParameterIoType.OUT",
                   ParameterIoType.OUT,
                   DatabaseUtil.getParameterIoType(ParameterIoType.OUT.getType()));
        // check
        assertSame("getParameterIoType () should return ParameterIoType.RET",
                   ParameterIoType.RET,
                   DatabaseUtil.getParameterIoType(ParameterIoType.RET.getType()));
        // check
        assertSame("getParameterIoType () should return ParameterIoType.RESULT",
                   ParameterIoType.RESULT,
                   DatabaseUtil.getParameterIoType(ParameterIoType.RESULT.getType()));
    }

    public void testGetPrivilegeType() {
        // check
        assertNull("getPrivilegeType (null) should return null ", DatabaseUtil.getPrivilegeType(null));
        // check
        assertSame("getPrivilegeType () should return PrivilegeType.INSERT",
                   PrivilegeType.INSERT,
                   DatabaseUtil.getPrivilegeType(PrivilegeType.INSERT.getType()));
        // check
        assertSame("getPrivilegeType () should return PrivilegeType.SELECT",
                   PrivilegeType.SELECT,
                   DatabaseUtil.getPrivilegeType(PrivilegeType.SELECT.getType()));
        // check
        assertSame("getPrivilegeType () should return PrivilegeType.UPDATE",
                   PrivilegeType.UPDATE,
                   DatabaseUtil.getPrivilegeType(PrivilegeType.UPDATE.getType()));
        // check
        assertSame("getPrivilegeType () should return PrivilegeType.DELETE",
                   PrivilegeType.DELETE,
                   DatabaseUtil.getPrivilegeType(PrivilegeType.DELETE.getType()));
        // check
        assertSame("getPrivilegeType () should return PrivilegeType.REFERENCE",
                   PrivilegeType.REFERENCE,
                   DatabaseUtil.getPrivilegeType(PrivilegeType.REFERENCE.getType()));
        // check
        assertSame("getPrivilegeType () should return PrivilegeType.OTHER",
                   PrivilegeType.OTHER,
                   DatabaseUtil.getPrivilegeType(PrivilegeType.OTHER.getType()));
    }

    public void testGetResultSetConcurrencyType() {
        // check
        assertNull("getResultSetConcurrencyType (null) should return null ", DatabaseUtil.getResultSetConcurrencyType(null));
        // check
        assertSame("getResultSetConcurrencyType () should return ResultSetConcurrencyType.READ_ONLY",
                   ResultSetConcurrencyType.READ_ONLY,
                   DatabaseUtil.getResultSetConcurrencyType(ResultSetConcurrencyType.READ_ONLY.getConcurrency()));
        // check
        assertSame("getResultSetConcurrencyType () should return ResultSetConcurrencyType.UPDATABLE",
                   ResultSetConcurrencyType.UPDATABLE,
                   DatabaseUtil.getResultSetConcurrencyType(ResultSetConcurrencyType.UPDATABLE.getConcurrency()));
    }

    public void testGetResultSetHoldabilityType() {
        // check
        assertNull("getResultSetHoldabilityType (null) should return null ", DatabaseUtil.getResultSetHoldabilityType(null));
        // check
        assertSame("getResultSetHoldabilityType () should return ResultSetHoldabilityType.HOLD_CURSORS_OVER_COMMIT",
                   ResultSetHoldabilityType.HOLD_CURSORS_OVER_COMMIT,
                   DatabaseUtil.getResultSetHoldabilityType(ResultSetHoldabilityType.HOLD_CURSORS_OVER_COMMIT.getHoldability()));
        // check
        assertSame("getResultSetHoldabilityType () should return ResultSetHoldabilityType.CLOSE_CURSORS_AT_COMMIT",
                   ResultSetHoldabilityType.CLOSE_CURSORS_AT_COMMIT,
                   DatabaseUtil.getResultSetHoldabilityType(ResultSetHoldabilityType.CLOSE_CURSORS_AT_COMMIT.getHoldability()));
    }

    public void testGetResultSetType() {
        // check
        assertNull("getResultSetType (null) should return null ", DatabaseUtil.getResultSetType(null));
        // check
        assertSame("getResultSetType () should return ResultSetType.FORWARD_ONLY ",
                   ResultSetType.FORWARD_ONLY,
                   DatabaseUtil.getResultSetType(ResultSetType.FORWARD_ONLY.getType()));
        // check
        assertSame("getResultSetType () should return ResultSetType.SCROLL_INSENSITIVE ",
                   ResultSetType.SCROLL_INSENSITIVE,
                   DatabaseUtil.getResultSetType(ResultSetType.SCROLL_INSENSITIVE.getType()));
        // check
        assertSame("getResultSetType () should return ResultSetType.SCROLL_SENSITIVE ",
                   ResultSetType.SCROLL_SENSITIVE,
                   DatabaseUtil.getResultSetType(ResultSetType.SCROLL_SENSITIVE.getType()));
    }

    public void testGetSearchabilityType() {
        // check
        assertNull("getSearchabilityType (null) should return null ", DatabaseUtil.getSearchabilityType(null));
        // check
        assertSame("getSearchabilityType () should return SearchabilityType.NOT_SUPPORTED ",
                   SearchabilityType.NOT_SUPPORTED,
                   DatabaseUtil.getSearchabilityType(SearchabilityType.NOT_SUPPORTED.getSearchability()));
        // check
        assertSame("getSearchabilityType () should return SearchabilityType.WHERE_LIKE ",
                   SearchabilityType.WHERE_LIKE,
                   DatabaseUtil.getSearchabilityType(SearchabilityType.WHERE_LIKE.getSearchability()));
        // check
        assertSame("getSearchabilityType () should return SearchabilityType.BASIC ",
                   SearchabilityType.BASIC,
                   DatabaseUtil.getSearchabilityType(SearchabilityType.BASIC.getSearchability()));
        // check
        assertSame("getSearchabilityType () should return SearchabilityType.SEARCHABLE ",
                   SearchabilityType.SEARCHABLE,
                   DatabaseUtil.getSearchabilityType(SearchabilityType.SEARCHABLE.getSearchability()));
    }

    public void testGetSortSequenceType() {
        // check
        assertNull("getSortSequenceType (null) should return null ", DatabaseUtil.getSortSequenceType(null));
        // check
        assertSame("getSortSequenceType () should return SortSequenceType.ASCENDING ",
                   SortSequenceType.ASCENDING,
                   DatabaseUtil.getSortSequenceType(SortSequenceType.ASCENDING.getType()));
        // check
        assertSame("getSortSequenceType () should return SortSequenceType.DESCENDING ",
                   SortSequenceType.DESCENDING,
                   DatabaseUtil.getSortSequenceType(SortSequenceType.DESCENDING.getType()));
        // check
        assertSame("getSortSequenceType () should return SortSequenceType.NOT_SUPPORTED ",
                   SortSequenceType.NOT_SUPPORTED,
                   DatabaseUtil.getSortSequenceType(SortSequenceType.NOT_SUPPORTED.getType()));
    }

    public void testGetSqlStateType() {
        // check
        assertNull("getSqlStateType (null) should return null ", DatabaseUtil.getSqlStateType(null));
        // check
        assertSame("getSqlStateType () should return SqlStateType.XOPEN ",
                   SQLStateType.XOPEN,
                   DatabaseUtil.getSqlStateType(SQLStateType.XOPEN.getState()));
        // check
        assertSame("getSqlStateType () should return SqlStateType.SQL99 ",
                   SQLStateType.SQL99,
                   DatabaseUtil.getSqlStateType(SQLStateType.SQL99.getState()));
    }

    public void testGetSqlType() {
        // check
        assertNull("getSqlType (null) should return null ", DatabaseUtil.getSqlType(null));
        // check
        assertSame("getSqlType () should return SqlType.BIT ", SqlType.BIT, DatabaseUtil.getSqlType(SqlType.BIT.getType()));
        // check
        assertSame("getSqlType () should return SqlType.TINYINT ",
                   SqlType.TINYINT,
                   DatabaseUtil.getSqlType(SqlType.TINYINT.getType()));
        // check
        assertSame("getSqlType () should return SqlType.SMALLINT ",
                   SqlType.SMALLINT,
                   DatabaseUtil.getSqlType(SqlType.SMALLINT.getType()));
        // check
        assertSame("getSqlType () should return SqlType.INTEGER ",
                   SqlType.INTEGER,
                   DatabaseUtil.getSqlType(SqlType.INTEGER.getType()));
        // check
        assertSame("getSqlType () should return SqlType.BIGINT ",
                   SqlType.BIGINT,
                   DatabaseUtil.getSqlType(SqlType.BIGINT.getType()));
        // check
        assertSame("getSqlType () should return SqlType.FLOAT ", SqlType.FLOAT, DatabaseUtil.getSqlType(SqlType.FLOAT.getType()));
        // check
        assertSame("getSqlType () should return SqlType.REAL ", SqlType.REAL, DatabaseUtil.getSqlType(SqlType.REAL.getType()));
        // check
        assertSame("getSqlType () should return SqlType.DOUBLE ",
                   SqlType.DOUBLE,
                   DatabaseUtil.getSqlType(SqlType.DOUBLE.getType()));
        // check
        assertSame("getSqlType () should return SqlType.NUMERIC ",
                   SqlType.NUMERIC,
                   DatabaseUtil.getSqlType(SqlType.NUMERIC.getType()));
        // check
        assertSame("getSqlType () should return SqlType.DECIMAL ",
                   SqlType.DECIMAL,
                   DatabaseUtil.getSqlType(SqlType.DECIMAL.getType()));
        // check
        assertSame("getSqlType () should return SqlType.CHAR ", SqlType.CHAR, DatabaseUtil.getSqlType(SqlType.CHAR.getType()));
        // check
        assertSame("getSqlType () should return SqlType.VARCHAR ",
                   SqlType.VARCHAR,
                   DatabaseUtil.getSqlType(SqlType.VARCHAR.getType()));
        // check
        assertSame("getSqlType () should return SqlType.LONGVARCHAR ",
                   SqlType.LONGVARCHAR,
                   DatabaseUtil.getSqlType(SqlType.LONGVARCHAR.getType()));
        // check
        assertSame("getSqlType () should return SqlType.DATE ", SqlType.DATE, DatabaseUtil.getSqlType(SqlType.DATE.getType()));
        // check
        assertSame("getSqlType () should return SqlType.TIME ", SqlType.TIME, DatabaseUtil.getSqlType(SqlType.TIME.getType()));
        // check
        assertSame("getSqlType () should return SqlType.TIMESTAMP ",
                   SqlType.TIMESTAMP,
                   DatabaseUtil.getSqlType(SqlType.TIMESTAMP.getType()));
        // check
        assertSame("getSqlType () should return SqlType.BINARY ",
                   SqlType.BINARY,
                   DatabaseUtil.getSqlType(SqlType.BINARY.getType()));
        // check
        assertSame("getSqlType () should return SqlType.VARBINARY ",
                   SqlType.VARBINARY,
                   DatabaseUtil.getSqlType(SqlType.VARBINARY.getType()));
        // check
        assertSame("getSqlType () should return SqlType.LONGVARBINARY ",
                   SqlType.LONGVARBINARY,
                   DatabaseUtil.getSqlType(SqlType.LONGVARBINARY.getType()));
        // check
        assertSame("getSqlType () should return SqlType.NULL ", SqlType.NULL, DatabaseUtil.getSqlType(SqlType.NULL.getType()));
        // check
        assertSame("getSqlType () should return SqlType.OTHER ", SqlType.OTHER, DatabaseUtil.getSqlType(SqlType.OTHER.getType()));
        // check
        assertSame("getSqlType () should return SqlType.JAVA_OBJECT ",
                   SqlType.JAVA_OBJECT,
                   DatabaseUtil.getSqlType(SqlType.JAVA_OBJECT.getType()));
        // check
        assertSame("getSqlType () should return SqlType.DISTINCT ",
                   SqlType.DISTINCT,
                   DatabaseUtil.getSqlType(SqlType.DISTINCT.getType()));
        // check
        assertSame("getSqlType () should return SqlType.STRUCT ",
                   SqlType.STRUCT,
                   DatabaseUtil.getSqlType(SqlType.STRUCT.getType()));
        // check
        assertSame("getSqlType () should return SqlType.ARRAY ", SqlType.ARRAY, DatabaseUtil.getSqlType(SqlType.ARRAY.getType()));
        // check
        assertSame("getSqlType () should return SqlType.BLOB ", SqlType.BLOB, DatabaseUtil.getSqlType(SqlType.BLOB.getType()));
        // check
        assertSame("getSqlType () should return SqlType.CLOB ", SqlType.CLOB, DatabaseUtil.getSqlType(SqlType.CLOB.getType()));
        // check
        assertSame("getSqlType () should return SqlType.REF ", SqlType.REF, DatabaseUtil.getSqlType(SqlType.REF.getType()));
        // check
        assertSame("getSqlType () should return SqlType.DATALINK ",
                   SqlType.DATALINK,
                   DatabaseUtil.getSqlType(SqlType.DATALINK.getType()));
        // check
        assertSame("getSqlType () should return SqlType.BOOLEAN ",
                   SqlType.BOOLEAN,
                   DatabaseUtil.getSqlType(SqlType.BOOLEAN.getType()));
    }

    public void testGetStoredProcedureResultType() {
        // check
        assertNull("getStoredProcedureResultType (null) should return null ", DatabaseUtil.getStoredProcedureResultType(null));
        // check
        assertSame("getStoredProcedureResultType () should return StoredProcedureResultType.NO_RESULT ",
                   StoredProcedureResultType.NO_RESULT,
                   DatabaseUtil.getStoredProcedureResultType(StoredProcedureResultType.NO_RESULT.getType()));
        // check
        assertSame("getStoredProcedureResultType () should return StoredProcedureResultType.RETURNS_RESULT ",
                   StoredProcedureResultType.RETURNS_RESULT,
                   DatabaseUtil.getStoredProcedureResultType(StoredProcedureResultType.RETURNS_RESULT.getType()));
        // check
        assertSame("getStoredProcedureResultType () should return StoredProcedureResultType.UNKNOWN ",
                   StoredProcedureResultType.UNKNOWN,
                   DatabaseUtil.getStoredProcedureResultType(StoredProcedureResultType.UNKNOWN.getType()));
    }

    public void testGetTransactionIsolationLevelType() {
        // check
        assertNull("getTransactionIsolationLevelType (null) should return null ",
                   DatabaseUtil.getTransactionIsolationLevelType(null));
        // check
        assertSame("getTransactionIsolationLevelType () should return TransactionIsolationLevelType.NONE",
                   TransactionIsolationLevelType.NONE,
                   DatabaseUtil.getTransactionIsolationLevelType(TransactionIsolationLevelType.NONE.getLevel()));
        // check
        assertSame("getTransactionIsolationLevelType () should return TransactionIsolationLevelType.READ_UNCOMMITTED",
                   TransactionIsolationLevelType.READ_UNCOMMITTED,
                   DatabaseUtil.getTransactionIsolationLevelType(TransactionIsolationLevelType.READ_UNCOMMITTED.getLevel()));
        // check
        assertSame("getTransactionIsolationLevelType () should return TransactionIsolationLevelType.READ_COMMITTED",
                   TransactionIsolationLevelType.READ_COMMITTED,
                   DatabaseUtil.getTransactionIsolationLevelType(TransactionIsolationLevelType.READ_COMMITTED.getLevel()));
        // check
        assertSame("getTransactionIsolationLevelType () should return TransactionIsolationLevelType.REPEATABLE_READ",
                   TransactionIsolationLevelType.REPEATABLE_READ,
                   DatabaseUtil.getTransactionIsolationLevelType(TransactionIsolationLevelType.REPEATABLE_READ.getLevel()));
        // check
        assertSame("getTransactionIsolationLevelType () should return TransactionIsolationLevelType.SERIALIZABLE",
                   TransactionIsolationLevelType.SERIALIZABLE,
                   DatabaseUtil.getTransactionIsolationLevelType(TransactionIsolationLevelType.SERIALIZABLE.getLevel()));
    }

    public void testGetStandardUserDefinedTypes() {
        // get string array
        String udtTypes = DatabaseUtil.getStandardUserDefinedTypes();
        // check not null
        assertNotNull("DatabaseUtil.getStandardUserDefinedTypes should return not empty list", udtTypes);
        // check that JAVA_OBJECT present
        assertTrue("JAVA_OBJECT should be specified among standard UDT", udtTypes.contains("JAVA_OBJECT"));
        // check that STRUCT present
        assertTrue("STRUCT should be specified among standard UDT", udtTypes.contains("STRUCT"));
        // check that DISTINCT present
        assertTrue("DISTINCT should be specified among standard UDT", udtTypes.contains("DISTINCT"));
    }

    public void testGetUserDefinedTypes() {
        // UDT string array
        String udtTypes = "DISTINCT,JAVA_OBJECT,STRUCT";
        // get int array
        int[] udtIntTypes = DatabaseUtil.getUserDefinedTypes(udtTypes);
        // check not null
        assertNotNull("UDT int array should be not null", udtIntTypes);
        // check size
        assertEquals("The size of UDT int array is expected to be three", 3, udtIntTypes.length);
        // check first element
        assertEquals("The udtIntTypes[0] should be equal to Types.DISTINCT", Types.DISTINCT, udtIntTypes[0]);
        // check second element
        assertEquals("The udtIntTypes[1] should be equal to Types.JAVA_OBJECT", Types.JAVA_OBJECT, udtIntTypes[1]);
        // check third element
        assertEquals("The udtIntTypes[2] should be equal to Types.STRUCT", Types.STRUCT, udtIntTypes[2]);
    }
}
