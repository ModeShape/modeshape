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
package org.jboss.dna.common.jdbc.util;

import java.lang.reflect.Method;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.PropertyUtils;
import org.jboss.dna.common.jdbc.JdbcMetadataI18n;
import org.jboss.dna.common.jdbc.model.ModelFactory;
import org.jboss.dna.common.jdbc.model.api.Attribute;
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifier;
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifierScopeType;
import org.jboss.dna.common.jdbc.model.api.Catalog;
import org.jboss.dna.common.jdbc.model.api.ColumnPseudoType;
import org.jboss.dna.common.jdbc.model.api.Database;
import org.jboss.dna.common.jdbc.model.api.DatabaseMetaDataMethodException;
import org.jboss.dna.common.jdbc.model.api.ForeignKey;
import org.jboss.dna.common.jdbc.model.api.ForeignKeyColumn;
import org.jboss.dna.common.jdbc.model.api.Index;
import org.jboss.dna.common.jdbc.model.api.IndexColumn;
import org.jboss.dna.common.jdbc.model.api.IndexType;
import org.jboss.dna.common.jdbc.model.api.KeyDeferrabilityType;
import org.jboss.dna.common.jdbc.model.api.KeyModifyRuleType;
import org.jboss.dna.common.jdbc.model.api.NullabilityType;
import org.jboss.dna.common.jdbc.model.api.Parameter;
import org.jboss.dna.common.jdbc.model.api.ParameterIoType;
import org.jboss.dna.common.jdbc.model.api.PrimaryKey;
import org.jboss.dna.common.jdbc.model.api.PrimaryKeyColumn;
import org.jboss.dna.common.jdbc.model.api.Privilege;
import org.jboss.dna.common.jdbc.model.api.PrivilegeType;
import org.jboss.dna.common.jdbc.model.api.Reference;
import org.jboss.dna.common.jdbc.model.api.ResultSetConcurrencyType;
import org.jboss.dna.common.jdbc.model.api.ResultSetHoldabilityType;
import org.jboss.dna.common.jdbc.model.api.ResultSetType;
import org.jboss.dna.common.jdbc.model.api.SQLStateType;
import org.jboss.dna.common.jdbc.model.api.Schema;
import org.jboss.dna.common.jdbc.model.api.SearchabilityType;
import org.jboss.dna.common.jdbc.model.api.SortSequenceType;
import org.jboss.dna.common.jdbc.model.api.SqlType;
import org.jboss.dna.common.jdbc.model.api.SqlTypeInfo;
import org.jboss.dna.common.jdbc.model.api.StoredProcedure;
import org.jboss.dna.common.jdbc.model.api.StoredProcedureResultType;
import org.jboss.dna.common.jdbc.model.api.Table;
import org.jboss.dna.common.jdbc.model.api.TableColumn;
import org.jboss.dna.common.jdbc.model.api.TableType;
import org.jboss.dna.common.jdbc.model.api.TransactionIsolationLevelType;
import org.jboss.dna.common.jdbc.model.api.UserDefinedType;
import org.jboss.dna.common.util.Logger;

/**
 * Database related utilities
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DatabaseUtil {
    /**
     * Logging for this instance
     */
    private static Logger log = Logger.getLogger(DatabaseUtil.class);

    /**
     * Standard UDT types as string
     */
    private static String standardUserDefinedTypes = "JAVA_OBJECT,STRUCT,DISTINCT";

    private static Map<String, Integer> standardUserDefinedTypesMapping = new HashMap<String, Integer>();

    static {
        standardUserDefinedTypesMapping.put("JAVA_OBJECT", new Integer(SqlType.JAVA_OBJECT.getType()));
        standardUserDefinedTypesMapping.put("STRUCT", new Integer(SqlType.STRUCT.getType()));
        standardUserDefinedTypesMapping.put("DISTINCT", new Integer(SqlType.DISTINCT.getType()));
    }

    // ~ Constructors ---------------------------------------------------------------------

    /**
     * Constructor
     */
    private DatabaseUtil() {
    }

    // ~ Methods --------------------------------------------------------------------------

    /**
     * Returns Standard UDT types as string
     * 
     * @return Standard UDT types as string
     */
    public static String getStandardUserDefinedTypes() {
        return standardUserDefinedTypes;
    }

    /**
     * Converts string array of UDT names to appropriate int array of UDT types specified in java.sql.Types
     * 
     * @param userDefinedTypes the string array of UDT names
     * @return int array of user defined types specified in java.sql.Types
     */
    public static int[] getUserDefinedTypes( String userDefinedTypes ) {
        // convert comma separated list of user defined types to array
        String[] userDefinedTypesStringArray = (userDefinedTypes == null) ? null : userDefinedTypes.split(",");
        // create UDT int array
        int[] userDefinedTypesArray = new int[userDefinedTypesStringArray.length];
        // fill array
        for (int i = 0; i < userDefinedTypesStringArray.length; i++) {
            // get value from map
            Integer udtType = standardUserDefinedTypesMapping.get(userDefinedTypesStringArray[i]);
            // set int value
            userDefinedTypesArray[i] = (udtType == null) ? Types.NULL : udtType.intValue();
        }
        return userDefinedTypesArray;
    }

    /**
     * Returns boolean with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @return boolean with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Boolean getBoolean( ResultSet resultSet,
                                      String columnName ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get boolean
        boolean value = resultSet.getBoolean(columnName);
        // return
        return (resultSet.wasNull()) ? null : (value == true) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Returns boolean with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @param failOnError if true raises exception
     * @return boolean with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Boolean getBoolean( ResultSet resultSet,
                                      String columnName,
                                      boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get boolean
        boolean value = false;
        try {
            value = resultSet.getBoolean(columnName);
            // return
            return (resultSet.wasNull()) ? null : (value == true) ? Boolean.TRUE : Boolean.FALSE;
        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnName, e.getMessage());
            return null;
        }
    }

    /**
     * Returns boolean with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @return boolean with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Boolean getBoolean( ResultSet resultSet,
                                      int columnIndex ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get boolean
        boolean value = resultSet.getBoolean(columnIndex);
        // return
        return (resultSet.wasNull()) ? null : (value == true) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Returns boolean with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @param failOnError if true raises exception
     * @return boolean with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Boolean getBoolean( ResultSet resultSet,
                                      int columnIndex,
                                      boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get boolean
        boolean value = false;
        try {
            value = resultSet.getBoolean(columnIndex);
            // return
            return (resultSet.wasNull()) ? null : (value == true) ? Boolean.TRUE : Boolean.FALSE;
        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnIndex, e.getMessage());
            return null;
        }
    }

    /**
     * Returns integer with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @return integer with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Integer getInteger( ResultSet resultSet,
                                      String columnName ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get integer
        int value = resultSet.getInt(columnName);
        // return
        return (resultSet.wasNull()) ? null : new Integer(value);
    }

    /**
     * Returns integer with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @param failOnError if true raises exception
     * @return integer with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Integer getInteger( ResultSet resultSet,
                                      String columnName,
                                      boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get
        int value = 0;
        try {
            value = resultSet.getInt(columnName);
            // return
            return (resultSet.wasNull()) ? null : new Integer(value);

        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnName, e.getMessage());
            return null;
        }
    }

    /**
     * Returns integer with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @return integer with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Integer getInteger( ResultSet resultSet,
                                      int columnIndex ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get integer
        int value = resultSet.getInt(columnIndex);
        // return
        return (resultSet.wasNull()) ? null : new Integer(value);
    }

    /**
     * Returns integer with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @param failOnError if true raises exception
     * @return integer with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Integer getInteger( ResultSet resultSet,
                                      int columnIndex,
                                      boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get
        int value = 0;
        try {
            value = resultSet.getInt(columnIndex);
            // return
            return (resultSet.wasNull()) ? null : new Integer(value);

        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnIndex, e.getMessage());
            return null;
        }
    }

    /**
     * Returns long with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @return long with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Long getLong( ResultSet resultSet,
                                String columnName ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get integer
        long value = resultSet.getLong(columnName);
        // return
        return (resultSet.wasNull()) ? null : new Long(value);
    }

    /**
     * Returns long with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @param failOnError if true raises exception
     * @return long with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Long getLong( ResultSet resultSet,
                                String columnName,
                                boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get
        long value = 0;
        try {
            value = resultSet.getLong(columnName);
            // return
            return (resultSet.wasNull()) ? null : new Long(value);

        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnName, e.getMessage());
            return null;
        }
    }

    /**
     * Returns long with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @return long with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Long getLong( ResultSet resultSet,
                                int columnIndex ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get integer
        long value = resultSet.getLong(columnIndex);
        // return
        return (resultSet.wasNull()) ? null : new Long(value);
    }

    /**
     * Returns long with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @param failOnError if true raises exception
     * @return long with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Long getLong( ResultSet resultSet,
                                int columnIndex,
                                boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get
        long value = 0;
        try {
            value = resultSet.getLong(columnIndex);
            // return
            return (resultSet.wasNull()) ? null : new Long(value);

        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnIndex, e.getMessage());
            return null;
        }
    }

    /**
     * Returns double with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @return double with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Double getDouble( ResultSet resultSet,
                                    String columnName ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get integer
        double value = resultSet.getDouble(columnName);
        // return
        return (resultSet.wasNull()) ? null : new Double(value);
    }

    /**
     * Returns double with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @param failOnError if true raises exception
     * @return double with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Double getDouble( ResultSet resultSet,
                                    String columnName,
                                    boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get
        double value = 0.0;
        try {
            value = resultSet.getLong(columnName);
            // return
            return (resultSet.wasNull()) ? null : new Double(value);

        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnName, e.getMessage());
            return null;
        }
    }

    /**
     * Returns double with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @return double with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Double getDouble( ResultSet resultSet,
                                    int columnIndex ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get integer
        double value = resultSet.getDouble(columnIndex);
        // return
        return (resultSet.wasNull()) ? null : new Double(value);
    }

    /**
     * Returns double with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @param failOnError if true raises exception
     * @return double with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static Double getDouble( ResultSet resultSet,
                                    int columnIndex,
                                    boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get
        double value = 0.0;
        try {
            value = resultSet.getLong(columnIndex);
            // return
            return (resultSet.wasNull()) ? null : new Double(value);

        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnIndex, e.getMessage());
            return null;
        }
    }

    /**
     * Returns String with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @return double with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static String getString( ResultSet resultSet,
                                    String columnName ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get integer
        String value = resultSet.getString(columnName);
        // return
        return (resultSet.wasNull()) ? null : value;
    }

    /**
     * Returns String with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnName the name of column
     * @param failOnError if true raises exception
     * @return double with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static String getString( ResultSet resultSet,
                                    String columnName,
                                    boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get
        String value = null;
        try {
            value = resultSet.getString(columnName);
            // return
            return (resultSet.wasNull()) ? null : value;

        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnName, e.getMessage());
            return null;
        }
    }

    /**
     * Returns String with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @return double with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static String getString( ResultSet resultSet,
                                    int columnIndex ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        // get integer
        String value = resultSet.getString(columnIndex);
        // return
        return (resultSet.wasNull()) ? null : value;
    }

    /**
     * Returns String with respect to NULL values (ResultSet.wasNull())
     * 
     * @param resultSet the result set to fetch from
     * @param columnIndex the index of column
     * @param failOnError if true raises exception
     * @return double with respect to NULL values (could be null)
     * @throws SQLException
     */
    public static String getString( ResultSet resultSet,
                                    int columnIndex,
                                    boolean failOnError ) throws SQLException {
        // check result set
        if (resultSet == null) {
            return null;
        }
        String value = null;
        try {
            value = resultSet.getString(columnIndex);
            // return
            return (resultSet.wasNull()) ? null : value;

        } catch (SQLException e) {
            if (failOnError) {
                throw e;
            }
            log.error(JdbcMetadataI18n.unableToGetValueFromColumn, columnIndex, e.getMessage());
            return null;
        }
    }

    /**
     * Returns BestRowIdentifierScopeType or null
     * 
     * @param type the best row identifier
     * @return BestRowIdentifierScopeType or null
     */
    public static BestRowIdentifierScopeType getBestRowIdentifierScopeType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (BestRowIdentifierScopeType sType : BestRowIdentifierScopeType.values()) {
            if (sType.getScope() == type.intValue()) {
                return sType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown best row identifier scope type %d", "getBestRowIdentifierScopeType", type));

        return null;
    }

    /**
     * Returns ColumnPseudoType or null
     * 
     * @param type the column pseudo type
     * @return ColumnPseudoType or null
     */
    public static ColumnPseudoType getColumnPseudoType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (ColumnPseudoType pType : ColumnPseudoType.values()) {
            if (pType.getType() == type.intValue()) {
                return pType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown column pseudo type %d", "getColumnPseudoType", type));

        return null;
    }

    /**
     * Returns IndexType or null
     * 
     * @param type the index type
     * @return IndexType or null
     */
    public static IndexType getIndexType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (IndexType iType : IndexType.values()) {
            if (iType.getType() == type.intValue()) {
                return iType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown index type %d", "getKeyDeferrabilityType", type));

        return null;
    }

    /**
     * Returns KeyDeferrabilityType or null
     * 
     * @param type the key defferability type
     * @return KeyDeferrabilityType or null
     */
    public static KeyDeferrabilityType getKeyDeferrabilityType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (KeyDeferrabilityType dType : KeyDeferrabilityType.values()) {
            if (dType.getDeferrability() == type.intValue()) {
                return dType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown key deferrability type %d", "getKeyDeferrabilityType", type));

        return null;
    }

    /**
     * Returns KeyModifyRuleType or null
     * 
     * @param type the key modify rule type
     * @return KeyModifyRuleType or null
     */
    public static KeyModifyRuleType getKeyModifyRuleType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (KeyModifyRuleType rType : KeyModifyRuleType.values()) {
            if (rType.getRule() == type.intValue()) {
                return rType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown key modify rule type %d", "getKeyModifyRuleType", type));

        return null;
    }

    /**
     * Returns column nullability, or null
     * 
     * @param type
     * @return NullabilityType
     */
    public static NullabilityType getNullabilityType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (NullabilityType nType : NullabilityType.values()) {
            if (nType.getNullability() == type.intValue()) {
                return nType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown nullability type %d", "getNullabilityType", type));

        return null;
    }

    /**
     * Returns ParameterIoType based on type, or null
     * 
     * @param type i/o type
     * @return ParameterIoType based on type, or null
     */
    public static ParameterIoType getParameterIoType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (ParameterIoType ioType : ParameterIoType.values()) {
            if (ioType.getType() == type.intValue()) {
                return ioType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown parameter I/O type %d", "getParameterIoType", type));

        return null;
    }

    /**
     * Returns PrivilegeType or null
     * 
     * @param type the privilege type
     * @return PrivilegeType or null
     */
    public static PrivilegeType getPrivilegeType( String type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (PrivilegeType pType : PrivilegeType.values()) {
            if (pType.getType().equals(type)) {
                return pType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown privilege type %s", "getPrivilegeType", type));

        return null;
    }

    /**
     * Returns ResultSetConcurrencyType or null
     * 
     * @param type the concurrency
     * @return ResultSetConcurrencyType or null
     */
    public static ResultSetConcurrencyType getResultSetConcurrencyType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (ResultSetConcurrencyType cType : ResultSetConcurrencyType.values()) {
            if (cType.getConcurrency() == type.intValue()) {
                return cType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown result set concurrency type %d", "getResultSetConcurrencyType", type));

        return null;
    }

    /**
     * Returns ResultSetHoldabilityType or null
     * 
     * @param type the holdability
     * @return ResultSetHoldabilityType or null
     */
    public static ResultSetHoldabilityType getResultSetHoldabilityType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (ResultSetHoldabilityType hType : ResultSetHoldabilityType.values()) {
            if (hType.getHoldability() == type.intValue()) {
                return hType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown result set holdability type %d", "getResultSetHoldabilityType", type));

        return null;
    }

    /**
     * Returns ResultSetType or null
     * 
     * @param type the result set type
     * @return ResultSetType or null
     */
    public static ResultSetType getResultSetType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (ResultSetType rsType : ResultSetType.values()) {
            if (rsType.getType() == type.intValue()) {
                return rsType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown result set type %d", "getResultSetType", type));

        return null;
    }

    /**
     * Returns SearchabilityType or null
     * 
     * @param type the searchability
     * @return SearchabilityType or null
     */
    public static SearchabilityType getSearchabilityType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (SearchabilityType sType : SearchabilityType.values()) {
            if (sType.getSearchability() == type.intValue()) {
                return sType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown serachability type %d", "getSearchabilityType", type));

        return null;
    }

    /**
     * Returns SortSequenceType or null
     * 
     * @param type the sort sequence
     * @return SortSequenceType or null
     */
    public static SortSequenceType getSortSequenceType( String type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for values
        for (SortSequenceType sType : SortSequenceType.values()) {
            if ((sType.getType() != null) && (sType.getType().equals(type))) {
                return sType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown sort sequence type %s", "getSortSequenceType", type));

        return null;
    }

    /**
     * Returns SqlStateType based on type, or null
     * 
     * @param type the SQL state type
     * @return SqlStateType based on type, or null
     */
    public static SQLStateType getSqlStateType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for Type values
        for (SQLStateType stateType : SQLStateType.values()) {
            if (stateType.getState() == type.intValue()) {
                return stateType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown SQL state type %d", "getSqlStateType", type));

        return null;
    }

    /**
     * Returns SqlType based on data type, or null
     * 
     * @param type the SQL type
     * @return SqlType based on data type, or null
     */
    public static SqlType getSqlType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for SqlType values
        for (SqlType sqlType : SqlType.values()) {
            if (sqlType.getType() == type.intValue()) {
                return sqlType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown SQL Type %d", "getSqlType", type));

        return null;
    }

    /**
     * Returns SP result type based on PROCEDURE_TYPE
     * 
     * @param type the stored procedure result type
     * @return SP result type based on PROCEDURE_TYPE, or null
     */
    public static StoredProcedureResultType getStoredProcedureResultType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for SqlType values
        for (StoredProcedureResultType spResultType : StoredProcedureResultType.values()) {
            if (spResultType.getType() == type.intValue()) {
                return spResultType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown stored procedure result type %d", "getStoredProcedureResultType", type));

        return null;
    }

    /**
     * Returns transaction isolation level, or null
     * 
     * @param type the level type
     * @return transaction isolation level, or null
     */
    public static TransactionIsolationLevelType getTransactionIsolationLevelType( Integer type ) {
        // check input
        if (type == null) {
            return null;
        }
        // check for Type values
        for (TransactionIsolationLevelType txType : TransactionIsolationLevelType.values()) {
            if (txType.getLevel() == type.intValue()) {
                return txType;
            }
        }
        // log only if not found
        log.debug(String.format("[%s] Unknown transaction isolation level type %d", "getTransactionIsolationLevelType", type));

        return null;
    }

    /**
     * Creates catalog based on current record in result set; adds catalog to the database
     * 
     * @param factory the model factory to create table
     * @param resultSet the table result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @return created catalog
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static Catalog populateCatalog( ModelFactory factory,
                                           ResultSet resultSet,
                                           Logger traceLog,
                                           boolean failOnError,
                                           Database database ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // get type catalog
        String catalogName = getString(resultSet, "TABLE_CAT", false);

        // create catalog object
        Catalog catalog = factory.createCatalog();

        // ***************************
        // *** DatabaseNamedObject ***
        // ***************************

        // set name
        catalog.setName(catalogName);
        // set remarks
        // catalog.setRemarks (remarks); // N/A
        // TODO set extra properties
        // catalog.addExtraProperty (String key, Object value);

        // ***************
        // *** Catalog ***
        // ***************

        // add catalog to the list
        database.addCatalog(catalog);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The catalog '%s' has been added.", database.getName(), catalogName));
        }

        return catalog;
    }

    /**
     * Creates schema based on current record in result set; adds schema to the database
     * 
     * @param factory the model factory to create table
     * @param resultSet the table result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @return created schema
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static Schema populateSchema( ModelFactory factory,
                                         ResultSet resultSet,
                                         Logger traceLog,
                                         boolean failOnError,
                                         Database database ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // get schema name
        String schemaName = getString(resultSet, "TABLE_SCHEM", false);

        // get catalog name
        String catalogName = getString(resultSet, "TABLE_CATALOG", false);

        // create schema object
        Schema schema = factory.createSchema();

        // ***************************
        // *** DatabaseNamedObject ***
        // ***************************

        // set name
        schema.setName(schemaName);
        // set remarks
        // schema.setRemarks (remarks); // N/A
        // TODO set extra properties
        // schema.addExtraProperty (String key, Object value);

        // ***************
        // *** Schema ***
        // ***************

        // check catalog name
        if (catalogName != null) {
            Catalog catalog = database.findCatalogByName(catalogName);
            schema.setCatalog(catalog);
        }

        // add schema to the list
        database.addSchema(schema);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The schema '%s' has been added.", database.getName(), schemaName));
        }

        return schema;
    }

    /**
     * Creates table types based on current record in result set; adds table types to the database
     * 
     * @param factory the model factory to create table
     * @param resultSet the table result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @return created schema
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */

    public static TableType populateTableType( ModelFactory factory,
                                               ResultSet resultSet,
                                               Logger traceLog,
                                               boolean failOnError,
                                               Database database ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // get table type
        String tableTypeName = getString(resultSet, "TABLE_TYPE", false);

        // create table type object
        TableType tableType = factory.createTableType();

        // ***************************
        // *** DatabaseNamedObject ***
        // ***************************

        // set name
        tableType.setName(tableTypeName);
        // set remarks
        // schema.setRemarks (remarks); // N/A
        // TODO set extra properties
        // schema.addExtraProperty (String key, Object value);

        // ***************
        // *** Table Type ***
        // ***************

        // add table type to the list
        database.addTableType(tableType);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The table type '%s' has been added.", database.getName(), tableType));
        }

        return tableType;
    }

    /**
     * Creates stored procedure based on current record in result set; adds SP to the database
     * 
     * @param factory the model factory to create SP
     * @param resultSet the stored procedure result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @return created SP
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static StoredProcedure populateStoredProcedure( ModelFactory factory,
                                                           ResultSet resultSet,
                                                           Logger traceLog,
                                                           boolean failOnError,
                                                           Database database ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // get procedure catalog
        String procedureCatalog = getString(resultSet, "PROCEDURE_CAT", false);
        // get procedure schema
        String procedureSchema = getString(resultSet, "PROCEDURE_SCHEM", false);
        // get procedure name
        String procedureName = getString(resultSet, "PROCEDURE_NAME", false);
        // get remarks
        String remarks = getString(resultSet, "REMARKS", false);
        // get kind of procedure
        Integer procedureType = getInteger(resultSet, "PROCEDURE_TYPE", false);

        // create Stored Procedure object
        StoredProcedure sp = factory.createStoredProcedure();

        // ***************************
        // *** DatabaseNamedObject ***
        // ***************************

        // set name
        sp.setName(procedureName);
        // set remarks
        sp.setRemarks(remarks);
        // TODO set extra properties
        // sp.addExtraProperty (String key, Object value);

        // ********************
        // *** SchemaObject ***
        // ********************

        // set catalog
        if ((procedureCatalog != null) && (procedureCatalog.trim().length() != 0)) {
            // find catalog
            Catalog catalog = database.findCatalogByName(procedureCatalog);
            // set catalog
            sp.setCatalog(catalog);
            // warn if null
            if (catalog == null) {
                traceLog.debug(String.format("[Database %s] Unable to find catalog '%4$s' for the procedure %s (schema %s, catalog %s)",
                                             database.getName(),
                                             procedureName,
                                             procedureSchema,
                                             procedureCatalog));
            }
            // if fail is enabled
            if (failOnError && (catalog == null)) {
                throw new DatabaseMetaDataMethodException("Catalog name shall be provided", "populateStoredProcedure");
            }
        }

        // set schema
        if ((procedureSchema != null) && (procedureSchema.trim().length() != 0)) {
            // find schema
            Schema schema = database.findSchemaByName(procedureCatalog, procedureSchema);
            // set schema
            sp.setSchema(schema);
            // warn if null
            if (schema == null) {
                traceLog.debug(String.format("[Database %s] Unable to find schema '%3$s' for the procedure %s (schema %s, catalog %s)",
                                             database.getName(),
                                             procedureName,
                                             procedureSchema,
                                             procedureCatalog));
            }
            // if fail is enabled
            if (failOnError && (schema == null)) {
                throw new DatabaseMetaDataMethodException("Schema name shall be provided", "populateStoredProcedure");
            }
        }

        // ***********************
        // *** StoredProcedure ***
        // ***********************

        // set sp result type
        sp.setResultType(getStoredProcedureResultType(procedureType));

        // add SP to the list
        database.addStoredProcedure(sp);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The Stored procedure '%s' (schema %s, catalog %s) has been added.",
                                         database.getName(),
                                         procedureName,
                                         procedureSchema,
                                         procedureCatalog));
        }

        return sp;
    }

    /**
     * Creates stored procedure parameter based on current record in result set; adds SP parameter to the SP
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param storedProcedure the owner stored procedure
     * @param ordinalPosition the parameter ordinal position
     * @return created SP parameter
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static Parameter populateStoredProcedureParameter( ModelFactory factory,
                                                              ResultSet resultSet,
                                                              Logger traceLog,
                                                              boolean failOnError,
                                                              Database database,
                                                              StoredProcedure storedProcedure,
                                                              int ordinalPosition ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (storedProcedure == null) {
            throw new IllegalArgumentException("storedProcedure");
        }
        // get procedure catalog
        String procedureCatalog = (storedProcedure.getCatalog() == null) ? null : storedProcedure.getCatalog().getName();
        // get procedure schema
        String procedureSchema = (storedProcedure.getSchema() == null) ? null : storedProcedure.getSchema().getName();
        // get procedure name
        String procedureName = storedProcedure.getName();

        // parameter name
        String parameterName = getString(resultSet, "COLUMN_NAME", false);
        // parameter i/o type
        Integer parameterType = getInteger(resultSet, "COLUMN_TYPE", false);
        // data type
        Integer dataType = getInteger(resultSet, "DATA_TYPE", false);
        // type name
        String typeName = getString(resultSet, "TYPE_NAME", false);
        // precision
        Integer precision = getInteger(resultSet, "PRECISION", false);
        // length
        Integer length = getInteger(resultSet, "LENGTH", false);
        // scale
        Integer scale = getInteger(resultSet, "SCALE", false);
        // radix
        Integer radix = getInteger(resultSet, "RADIX", false);
        // nullable
        Integer nullableType = getInteger(resultSet, "NULLABLE", false);
        // remarks
        String remarks = getString(resultSet, "REMARKS", false);

        // create Stored Procedure Parameter object
        Parameter parameter = factory.createParameter();

        // ***************************************
        // *** DatabaseNamedObject properties ***
        // ***************************************

        // name
        parameter.setName(parameterName);
        // remarks
        parameter.setRemarks(remarks);
        // TODO set extra properties
        // parameter.addExtraProperty (String key, Object value);

        // ***************
        // *** Column ***
        // ***************

        // owner
        parameter.setOwner(storedProcedure);
        // nullability
        parameter.setNullabilityType(getNullabilityType(nullableType));
        // SQL type
        parameter.setSqlType(getSqlType(dataType));
        // type name
        parameter.setTypeName(typeName);
        // Size
        parameter.setSize(length);
        // precision
        parameter.setPrecision(precision);
        // Radix
        parameter.setRadix(radix);
        // DefaultValue
        parameter.setDefaultValue(null); // not defined among standard columns
        // OrdinalPosition
        parameter.setOrdinalPosition(ordinalPosition);
        // CharOctetLength
        parameter.setCharOctetLength(null); // N/A
        // addPrivilege
        // parameter.addPrivilege (privilege); // N/A

        // *****************
        // *** Parameter ***
        // *****************

        // i/o type
        parameter.setIoType(getParameterIoType(parameterType));
        // scale
        parameter.setScale(scale);

        // add parameter to the SP
        storedProcedure.addParameter(parameter);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The Stored procedure '%s' (schema %s, catalog %s) parameter %s has been added.",
                                         database.getName(),
                                         procedureName,
                                         procedureSchema,
                                         procedureCatalog,
                                         parameterName));
        }

        return parameter;
    }

    /**
     * Creates table based on current record in result set; adds table to the database
     * 
     * @param factory the model factory to create table
     * @param resultSet the table result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @return created table
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static Table populateTable( ModelFactory factory,
                                       ResultSet resultSet,
                                       Logger traceLog,
                                       boolean failOnError,
                                       Database database ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // get catalog
        String tableCatalog = getString(resultSet, "TABLE_CAT", false);

        // get schema
        String tableSchema = getString(resultSet, "TABLE_SCHEM", false);

        // get name
        String tableName = getString(resultSet, "TABLE_NAME", false);
        // get type
        String tableTypeName = getString(resultSet, "TABLE_TYPE", false);
        // get remarks
        String remarks = getString(resultSet, "REMARKS", false);
        // get type catalog
        String typeCatalogName = getString(resultSet, "TYPE_CAT", false);

        // get type schema
        String typeSchemaName = getString(resultSet, "TYPE_SCHEM", false);
        // get type name
        String typeName = getString(resultSet, "TYPE_NAME", false);
        // get self reeferencing column name
        String selfRefColumnName = getString(resultSet, "SELF_REFERENCING_COL_NAME", false);
        // get ref generation
        String refGeneration = getString(resultSet, "REF_GENERATION", false);

        // create Table object
        Table table = factory.createTable();

        // ***************************
        // *** DatabaseNamedObject ***
        // ***************************

        // set name
        table.setName(tableName);
        // set remarks
        table.setRemarks(remarks);
        // TODO set extra properties
        // table.addExtraProperty (String key, Object value);

        // ********************
        // *** SchemaObject ***
        // ********************

        // set catalog
        if ((tableCatalog != null) && (tableCatalog.trim().length() != 0)) {
            // find catalog
            Catalog catalog = database.findCatalogByName(tableCatalog);
            // set catalog
            table.setCatalog(catalog);
            // warn if null
            if (catalog == null) {
                traceLog.debug(String.format("[Database %s] Unable to find catalog '%4$s' for the table %s (schema %s, catalog %s)",
                                             database.getName(),
                                             tableName,
                                             tableSchema,
                                             tableCatalog));
            }
            // if fail is enabled
            if (failOnError) {
                throw new DatabaseMetaDataMethodException("Catalog name shall be provided", "populateTable");

            }
        }

        // set schema
        if ((tableSchema != null) && (tableSchema.trim().length() != 0)) {
            // find schema
            Schema schema = database.findSchemaByName(tableCatalog, tableSchema);
            // set schema
            table.setSchema(schema);
            // warn if null
            if (schema == null) {
                traceLog.debug(String.format("[Database %s] Unable to find schema '%3$s' for the table %s (schema %s, catalog %s)",
                                             database.getName(),
                                             tableName,
                                             tableSchema,
                                             tableCatalog));
            }
            // if fail is enabled
            if (failOnError) {
                throw new DatabaseMetaDataMethodException("Table name shall be provided", "populateTable");
            }
        }

        // **************
        // *** Table ***
        // **************

        // set table type
        if (tableTypeName != null) {
            // create
            TableType tableType = factory.createTableType();
            // fill
            tableType.setName(tableTypeName);
            // set type
            table.setTableType(tableType);
        }

        // set catalog
        if ((typeCatalogName != null) && (typeCatalogName.trim().length() != 0)) {
            // find catalog
            Catalog typeCatalog = database.findCatalogByName(typeCatalogName);
            // set type catalog
            table.setTypeCatalog(typeCatalog);
            // warn if null
            if (typeCatalog == null) {
                traceLog.debug(String.format("[Database %s] Unable to find catalog '%4$s' for the table %s (schema %s, catalog %s)",
                                             database.getName(),
                                             tableName,
                                             tableSchema,
                                             typeCatalogName));
            }
        }

        // set schema
        if ((typeSchemaName != null) && (typeSchemaName.trim().length() != 0)) {
            // find schema
            Schema typeSchema = database.findSchemaByName(typeCatalogName, typeSchemaName);
            // set schema
            table.setTypeSchema(typeSchema);
            // warn if null
            if (typeSchema == null) {
                traceLog.debug(String.format("[Database %s] Unable to find schema '%3$s' for the table %s (schema %s, catalog %s)",
                                             database.getName(),
                                             tableName,
                                             typeSchemaName,
                                             typeCatalogName));
            }
            // if fail is enabled
            if (failOnError && (typeSchema == null)) {
                throw new DatabaseMetaDataMethodException("Schema name shall be provided", "populateTable");
            }
        }

        // set type name
        table.setTypeName(typeName);
        // set self referencing column name
        table.setSelfReferencingColumnName(selfRefColumnName);
        // set reference generation
        table.setReferenceGeneration(refGeneration);

        // add table to the list
        database.addTable(table);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) has been added.",
                                         database.getName(),
                                         tableName,
                                         tableSchema,
                                         tableCatalog));
        }

        return table;
    }

    /**
     * Creates table column based on current record in result set; adds column to the table
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param table the owner table
     * @return created table column
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static TableColumn populateTableColumn( ModelFactory factory,
                                                   ResultSet resultSet,
                                                   Logger traceLog,
                                                   boolean failOnError,
                                                   Database database,
                                                   Table table ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (table == null) {
            throw new IllegalArgumentException("table");
        }
        // get catalog name
        String catalogName = (table.getCatalog() == null) ? null : table.getCatalog().getName();
        // get schema name
        String schemaName = (table.getSchema() == null) ? null : table.getSchema().getName();
        // get table name
        String tableName = table.getName();

        // column name
        String columnName = getString(resultSet, "COLUMN_NAME", false);
        // data type
        Integer dataType = getInteger(resultSet, "DATA_TYPE", false);
        // type name
        String typeName = getString(resultSet, "TYPE_NAME", false);
        // size
        Integer size = getInteger(resultSet, "COLUMN_SIZE", false);
        // precision
        Integer precision = getInteger(resultSet, "DECIMAL_DIGITS", false);
        // radix
        Integer radix = getInteger(resultSet, "NUM_PREC_RADIX", false);
        // nullable
        Integer nullableType = getInteger(resultSet, "NULLABLE", false);
        // remarks
        String remarks = getString(resultSet, "REMARKS", false);
        // default value
        String defaultValue = getString(resultSet, "COLUMN_DEF", false);
        // character Octet Length
        Integer charOctetLength = getInteger(resultSet, "CHAR_OCTET_LENGTH", false);
        // ordinal position
        Integer ordinalPosition = getInteger(resultSet, "ORDINAL_POSITION", false);
        // is nullable string
        // String isNullableString = getString(resultSet, "IS_NULLABLE", false);
        // scope catalog
        String scopeCatalog = getString(resultSet, "SCOPE_CATLOG", false);
        // scope schema
        String scopeSchema = getString(resultSet, "SCOPE_SCHEMA", false);
        // scope table
        String scopeTable = getString(resultSet, "SCOPE_TABLE", false);
        // sourceDataType
        Integer sourceDataType = getInteger(resultSet, "SOURCE_DATA_TYPE", false);

        // create table column object
        TableColumn column = factory.createTableColumn();

        // ***************************************
        // *** DatabaseNamedObject properties ***
        // ***************************************

        // name
        column.setName(columnName);
        // remarks
        column.setRemarks(remarks);
        // TODO set extra properties
        // column.addExtraProperty (String key, Object value);

        // ***************
        // *** Column ***
        // ***************

        // owner
        column.setOwner(table);
        // nullability. The isNullableString is not used so far
        column.setNullabilityType(getNullabilityType(nullableType));
        // SQL type
        column.setSqlType(getSqlType(dataType));
        // type name
        column.setTypeName(typeName);
        // Size
        column.setSize(size);
        // precision
        column.setPrecision(precision);
        // Radix
        column.setRadix(radix);
        // DefaultValue
        column.setDefaultValue(defaultValue);
        // OrdinalPosition
        column.setOrdinalPosition(ordinalPosition);
        // CharOctetLength
        column.setCharOctetLength(charOctetLength);
        // addPrivilege
        // column.addPrivilege (privilege); //

        // ********************
        // *** Table Column ***
        // ********************

        // pseudo type
        column.setPseudoType(ColumnPseudoType.NOT_PSEUDO);

        // set reference
        if ((scopeCatalog != null) || (scopeSchema != null) || (scopeTable != null) || (sourceDataType != null)) {
            // create reference
            Reference reference = factory.createReference();
            // set Source Data Type
            reference.setSourceDataType(getSqlType(sourceDataType));
            // find table and set as source
            reference.setSourceTable(database.findTableByName(scopeCatalog, scopeSchema, scopeTable));

            // set reference
            column.setReference(reference);
        }

        // add column to the table
        table.addColumn(column);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) column %s has been added.",
                                         database.getName(),
                                         tableName,
                                         schemaName,
                                         catalogName,
                                         columnName));
        }

        return column;
    }

    /**
     * Creates table best row identifier based on current record in result set; adds best row identifier to the table
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param table the owner table
     * @return created table best row identifier
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static BestRowIdentifier populateBestRowIdentifier( ModelFactory factory,
                                                               ResultSet resultSet,
                                                               Logger traceLog,
                                                               boolean failOnError,
                                                               Database database,
                                                               Table table ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (table == null) {
            throw new IllegalArgumentException("table");
        }
        // get catalog name
        String catalogName = (table.getCatalog() == null) ? null : table.getCatalog().getName();
        // get schema name
        String schemaName = (table.getSchema() == null) ? null : table.getSchema().getName();
        // get table name
        String tableName = table.getName();

        // scope type
        Integer scope = getInteger(resultSet, "SCOPE", false);
        // column name
        String columnName = getString(resultSet, "COLUMN_NAME", false);
        // data type
        Integer dataType = getInteger(resultSet, "DATA_TYPE", false);
        // type name
        String typeName = getString(resultSet, "TYPE_NAME", false);
        // precision
        Integer precision = getInteger(resultSet, "COLUMN_SIZE", false);
        // scale
        Integer scale = getInteger(resultSet, "DECIMAL_DIGITS", false);
        // pseudoColumn
        Integer pseudoColumn = getInteger(resultSet, "PSEUDO_COLUMN", false);

        // *************************************
        // *** BestRowIdentifier properties ***
        // *************************************

        // get scope type
        BestRowIdentifierScopeType scopeType = getBestRowIdentifierScopeType(scope);

        // check not null
        if (scopeType == null) {
            // if exception generation is enabled then raise exception - invalid scope
            if (failOnError == true) {
                throw new IllegalArgumentException("scopeType");
            }
            return null;
        }

        // find table best row identifier object
        BestRowIdentifier brId = table.findBestRowIdentifierByScopeType(scopeType);
        // check if null
        if (brId == null) {
            // create
            brId = factory.createBestRowIdentifier();
        }

        // set scope type
        brId.setScopeType(scopeType);

        // determine if current record shows pseudo column
        boolean isPseudoColumn = ((getColumnPseudoType(pseudoColumn) != null) && (getColumnPseudoType(pseudoColumn) == ColumnPseudoType.PSEUDO));

        TableColumn column = null;

        if (isPseudoColumn) {
            // create
            column = factory.createTableColumn();

            // ***************************************
            // *** DatabaseNamedObject properties ***
            // ***************************************

            // name
            column.setName(columnName);
            // remarks
            // column.setRemarks (remarks); // N/A
            // TODO set extra properties
            // column.addExtraProperty (String key, Object value);

            // ***************
            // *** Column ***
            // ***************

            // owner
            column.setOwner(table);
            // SQL type
            column.setSqlType(getSqlType(dataType));
            // type name
            column.setTypeName(typeName);
            // precision
            column.setPrecision(precision);
            // size
            column.setSize(precision);
            // scale
            column.setRadix(scale);
            // pseudo type
            column.setPseudoType(getColumnPseudoType(pseudoColumn));
            // add to the table
            table.addColumn(column);
        } else {
            // trying to find column
            column = table.findColumnByName(columnName);

            // if column exists
            if (column != null) {
                // pseudo type
                column.setPseudoType(getColumnPseudoType(pseudoColumn));
            }
        }

        // if column exists
        if (column != null) {
            // add to the best row identifier
            brId.addColumn(column);
        }

        // add best row identifier to the table
        table.addBestRowIdentifier(brId);

        // get scope type string
        String scopeName = (brId.getScopeType() == null) ? null : brId.getScopeType().getName();

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) best row identifier with scope %s has been added.",
                                         database.getName(),
                                         tableName,
                                         schemaName,
                                         catalogName,
                                         scopeName));
        }

        return brId;
    }

    /**
     * Creates table's primary key based on ALL records in result set; adds pk columns to the table's primary key
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param table the owner table
     * @return created primary key
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static PrimaryKey populatePrimaryKey( ModelFactory factory,
                                                 ResultSet resultSet,
                                                 Logger traceLog,
                                                 boolean failOnError,
                                                 Database database,
                                                 Table table ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (table == null) {
            throw new IllegalArgumentException("table");
        }
        // get catalog name
        String catalogName = (table.getCatalog() == null) ? null : table.getCatalog().getName();
        // get schema name
        String schemaName = (table.getSchema() == null) ? null : table.getSchema().getName();
        // get table name
        String tableName = table.getName();
        // pk name
        String primaryKeyName = null; // initially null

        // create table's primary key
        PrimaryKey pk = factory.createPrimaryKey();

        // ***************************
        // *** DatabaseNamedObject ***
        // ***************************

        // set name
        // pk.setName (primaryKeyName); // it will be overriden later in this code
        // set remarks
        // pk.setRemarks (remarks); // N/A

        // TODO set extra properties
        // table.addExtraProperty (String key, Object value);

        // ********************
        // *** SchemaObject ***
        // ********************

        // set catalog
        pk.setCatalog(table.getCatalog());
        // set schema
        pk.setSchema(table.getSchema());

        // process all columns included into primary key
        while (resultSet.next()) {
            // get PK name
            if (primaryKeyName == null) {
                primaryKeyName = getString(resultSet, "PK_NAME", false);
                // set name
                pk.setName(primaryKeyName);
            }

            // column name
            String columnName = getString(resultSet, "COLUMN_NAME", false);
            // sequence number within primary key
            Integer ordinalPosition = getInteger(resultSet, "KEY_SEQ", false);

            // trying to find table column with specified name
            TableColumn tableColumn = table.findColumnByName(columnName);

            String errMessage = null;
            // warn if null
            if (tableColumn == null) {
                errMessage = String.format("[Database %s] Unable to find table column '%5$s' for the table %s (schema %s, catalog %s)",
                                           database.getName(),
                                           tableName,
                                           schemaName,
                                           catalogName,
                                           columnName);
                traceLog.debug(errMessage);
            }
            // if fail is enabled
            if (failOnError && (tableColumn == null)) {
                throw new DatabaseMetaDataMethodException(errMessage, "populatePrimaryKey");
            }

            // create PK column
            PrimaryKeyColumn pkColumn = factory.createPrimaryKeyColumn();
            // check if we found the original table column
            if (tableColumn != null) {
                // mark original table column as part of PK
                tableColumn.setPrimaryKeyColumn(Boolean.TRUE);
                // clone properties from original table column to the pkcolumn
                PropertyUtils.copyProperties(pkColumn, tableColumn);
            } else { // recovery if table column is not found but we still want to create pk column
                // set name at least
                pkColumn.setName(columnName);
            }
            // modify ordinal position that correspond to the position in PK
            pkColumn.setOrdinalPosition(ordinalPosition);

            // add PK column to the primary key
            pk.addColumn(pkColumn);
        }

        // set table primary key
        table.setPrimaryKey(pk);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) primary key %s has been added.",
                                         database.getName(),
                                         tableName,
                                         schemaName,
                                         catalogName,
                                         primaryKeyName));
        }

        return pk;
    }

    /**
     * Creates table's foreign keys based on ALL records in result set; adds fk columns to the table's foreing key
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param table the owner table
     * @return created list of foreign keys
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static Set<ForeignKey> populateForeignKeys( ModelFactory factory,
                                                       ResultSet resultSet,
                                                       Logger traceLog,
                                                       boolean failOnError,
                                                       Database database,
                                                       Table table ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (table == null) {
            throw new IllegalArgumentException("table");
        }
        // get catalog name
        String catalogName = (table.getCatalog() == null) ? null : table.getCatalog().getName();
        // get schema name
        String schemaName = (table.getSchema() == null) ? null : table.getSchema().getName();
        // get table name
        String tableName = table.getName();

        // create table's foreign key
        ForeignKey fk = factory.createForeignKey();

        // process all columns included into foreign key
        while (resultSet.next()) {
            // primary key table catalog being imported
            String pkTableCatalogName = getString(resultSet, "PKTABLE_CAT", false);
            // primary key table schema being imported
            String pkTableSchemaName = getString(resultSet, "PKTABLE_SCHEM", false);
            // primary key table name being imported
            String pkTableName = getString(resultSet, "PKTABLE_NAME", false);
            // primary key column name being imported
            String pkColumnName = getString(resultSet, "PKCOLUMN_NAME", false);

            // FK table name, schema and catalog are already known, so it is useless to fetch

            // foreign key column name
            String fkColumnName = getString(resultSet, "FKCOLUMN_NAME", false);
            // sequence number within foreign key
            Integer ordinalPosition = getInteger(resultSet, "KEY_SEQ", false);
            // update rule - What happens to a foreign key when the primary key is updated
            Integer updateRule = getInteger(resultSet, "UPDATE_RULE", false);
            // delete rule - What happens to the foreign key when primary is deleted
            Integer deleteRule = getInteger(resultSet, "DELETE_RULE", false);
            // foreign key name
            String foreignKeyName = getString(resultSet, "FK_NAME", false);
            // primary key name
            //String primaryKeyName = getString(resultSet, "PK_NAME", false);
            // can the evaluation of foreign key constraints be deferred until commit
            Integer defferability = getInteger(resultSet, "DEFERRABILITY", false);

            //
            // check if FK name has been set earlier and current record shows different FK -
            // so we need to add FK (previous) to the table
            //
            if ((fk.getName() != null) && (!fk.getName().equals(foreignKeyName))) {
                // add previous FK to the table
                table.addForeignKey(fk);

                // log
                if (traceLog.isTraceEnabled()) {
                    traceLog.trace(String.format("[Database %s] The table '%s' (schema %s, catalog %s) foreign key %s has been added.",
                                                 database.getName(),
                                                 tableName,
                                                 schemaName,
                                                 catalogName,
                                                 foreignKeyName));
                }

                // create new FK if a record is not last
                fk = factory.createForeignKey();
            } else {

                // ***************************
                // *** DatabaseNamedObject ***
                // ***************************

                // set FK name
                fk.setName(foreignKeyName);
                // set remarks
                // fk.setRemarks (remarks); // N/A

                // TODO set extra properties
                // fk.addExtraProperty (String key, Object value);

                // ********************
                // *** SchemaObject ***
                // ********************

                // set catalog
                fk.setCatalog(table.getCatalog());
                // set schema
                fk.setSchema(table.getSchema());

                // ***************************
                // *** ForeignKey ***
                // ***************************

                // trying to find table column with specified name
                TableColumn tableColumn = table.findColumnByName(fkColumnName);

                String errMessage = null;
                // warn if null
                if (tableColumn == null) {
                    errMessage = String.format("[Database %s] Unable to find table column '%5$s' for the table %s (schema %s, catalog %s)",
                                               database.getName(),
                                               tableName,
                                               schemaName,
                                               catalogName,
                                               fkColumnName);
                    traceLog.debug(errMessage);
                }
                // if fail is enabled
                if (failOnError && (tableColumn == null)) {
                    throw new DatabaseMetaDataMethodException(errMessage, "populateForeignKeys");
                }

                // create FK column
                ForeignKeyColumn fkColumn = factory.createForeignKeyColumn();

                // check if we found the original table column
                if (tableColumn != null) {
                    // mark original table column as part of FK
                    tableColumn.setForeignKeyColumn(Boolean.TRUE);
                    // clone properties from original table column to the fkcolumn
                    PropertyUtils.copyProperties(fkColumn, tableColumn);
                } else { // recovery if table column is not found but we still want to create fk column
                    // set name at least
                    fkColumn.setName(fkColumnName);
                }
                // modify ordinal position that correspond to the position in FK
                fkColumn.setOrdinalPosition(ordinalPosition);

                // check for PK table and corresponding PK column
                Table pkTable = database.findTableByName(pkTableCatalogName, pkTableSchemaName, pkTableName);
                // sets the scope table of a foreign key.
                fk.setSourceTable(pkTable);
                // find PK
                PrimaryKey pk = (pkTable == null) ? null : pkTable.getPrimaryKey();
                // set
                fk.setSourcePrimaryKey(pk);

                // What happens to a foreign key when the primary key is updated
                fk.setUpdateRule(getKeyModifyRuleType(updateRule));
                // What happens to a foreign key when the primary key is deleted
                fk.setDeleteRule(getKeyModifyRuleType(deleteRule));
                // Can the evaluation of foreign key constraints be deferred until commit
                fk.setDeferrability(getKeyDeferrabilityType(defferability));

                // find PK table column
                TableColumn pkColumn = (pkTable == null) ? null : pkTable.findColumnByName(pkColumnName);
                // Sets mapped source column (in PK/source table) for this foreign key column
                fkColumn.setSourceColumn(pkColumn);

                // add FK column to the foreign key
                fk.addColumn(fkColumn);
            }
        }

        // add FK to the table
        table.addForeignKey(fk);
        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) foreign key %s has been added.",
                                         database.getName(),
                                         tableName,
                                         schemaName,
                                         catalogName,
                                         fk.getName()));
        }

        // return set of created FK
        return table.getForeignKeys();
    }

    /**
     * Creates table's indexes based on ALL records in result set; adds columns to the table's index
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param table the owner table
     * @return created list of index
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static Set<Index> populateIndexes( ModelFactory factory,
                                              ResultSet resultSet,
                                              Logger traceLog,
                                              boolean failOnError,
                                              Database database,
                                              Table table ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (table == null) {
            throw new IllegalArgumentException("table");
        }
        // get catalog name
        String catalogName = (table.getCatalog() == null) ? null : table.getCatalog().getName();
        // get schema name
        String schemaName = (table.getSchema() == null) ? null : table.getSchema().getName();
        // get table name
        String tableName = table.getName();

        // create table's index
        Index index = factory.createIndex();

        // process all columns included into index
        while (resultSet.next()) {
            // table name, schema and catalog are already known, so it is useless to fetch

            // Can index values be non-unique
            Boolean nonUnique = getBoolean(resultSet, "NON_UNIQUE", false);
            // index catalog
            String indexQualifier = getString(resultSet, "INDEX_QUALIFIER", false);
            // index name ; null when TYPE is tableIndexStatistic
            String indexName = getString(resultSet, "INDEX_NAME", false);
            // index type
            Integer indexType = getInteger(resultSet, "TYPE", false);
            // sequence number within index
            Integer ordinalPosition = getInteger(resultSet, "ORDINAL_POSITION", false);
            // index column name
            String indexColumnName = getString(resultSet, "COLUMN_NAME", false);
            // column sort sequence, "A" => ascending, "D" => descending, may be null if sort sequence is not supported;
            // null when TYPE is tableIndexStatistic
            String ascOrDesc = getString(resultSet, "ASC_OR_DESC", false);
            // cardinality; When TYPE is tableIndexStatistic, then this is the number of rows in the table;
            // otherwise, it is the number of unique values in the index.
            Integer cardinality = getInteger(resultSet, "CARDINALITY", false);
            // pages; When TYPE is tableIndexStatisic then this is the number of pages used for the table,
            // otherwise it is the number of pages used for the current index.
            Integer pages = getInteger(resultSet, "PAGES", false);
            // filter condition if any (may be null)
            String filterCondition = getString(resultSet, "FILTER_CONDITION", false);

            //
            // check if index name has been set earlier and current record shows different index -
            // so we need to add index (previous) to the table
            //
            if ((index.getName() != null) && (!index.getName().equals(indexName))) {
                // add previous FK to the table
                table.addIndex(index);

                // log
                if (traceLog.isDebugEnabled()) {
                    traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) index %s has been added.",
                                                 database.getName(),
                                                 tableName,
                                                 schemaName,
                                                 catalogName,
                                                 indexName));
                }

                // create new index if a record is not last
                index = factory.createIndex();
            } else {

                // ***************************
                // *** DatabaseNamedObject ***
                // ***************************

                // set name
                index.setName(indexName);
                // set remarks
                // index.setRemarks (remarks); // N/A

                // TODO set extra properties
                // index.addExtraProperty (String key, Object value);

                // ********************
                // *** SchemaObject ***
                // ********************

                // set catalog; index catalog me be defined by indexQualifier
                index.setCatalog((indexQualifier == null) ? table.getCatalog() : database.findCatalogByName(indexQualifier));
                // set schema
                index.setSchema(table.getSchema());

                // ***************************
                // *** Index ***
                // ***************************

                // set unique as inversion of non unique
                index.setUnique(nonUnique == null ? null : (nonUnique.booleanValue() == true) ? Boolean.FALSE : Boolean.TRUE);
                // set Index Type
                index.setIndexType(getIndexType(indexType));
                // set Cardinality
                index.setCardinality(cardinality);
                // set Pages
                index.setPages(pages);
                // set filter condition
                index.setFilterCondition(filterCondition);

                // trying to find table column with specified name
                TableColumn tableColumn = table.findColumnByName(indexColumnName);

                String errMessage = null;
                // warn if null
                if (tableColumn == null) {
                    errMessage = String.format("[Database %s] Unable to find table column '%5$s' for the table %s (schema %s, catalog %s)",
                                               database.getName(),
                                               tableName,
                                               schemaName,
                                               catalogName,
                                               indexColumnName);
                    traceLog.debug(errMessage);
                }
                // if fail is enabled
                if (failOnError && (tableColumn == null)) {
                    throw new DatabaseMetaDataMethodException(errMessage, "populateIndexes");

                }

                // create index column
                IndexColumn indexColumn = factory.createIndexColumn();

                // check if we found the original table column
                if (tableColumn != null) {
                    // mark original table column as part of index
                    tableColumn.setIndexColumn(Boolean.TRUE);
                    // clone properties from original table column to the index column
                    PropertyUtils.copyProperties(indexColumn, tableColumn);
                } else { // recovery if table column is not found but we still want to create index column
                    // set name at least
                    indexColumn.setName(indexColumnName);
                }
                // modify ordinal position that correspond to the position in index
                indexColumn.setOrdinalPosition(ordinalPosition);
                // sort sequence type
                indexColumn.setSortSequenceType(getSortSequenceType(ascOrDesc));

                // add index column to the index
                index.addColumn(indexColumn);
            }

        }

        // add index to the table
        table.addIndex(index);
        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) index %s has been added.",
                                         database.getName(),
                                         tableName,
                                         schemaName,
                                         catalogName,
                                         index.getName()));
        }

        // return set of created indexes
        return table.getIndexes();
    }

    /**
     * Creates table's version column (if pseudo) based on current record in result set; adds column to the table
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param table the owner table
     * @return created/updated version table column
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static TableColumn populateVersionColumn( ModelFactory factory,
                                                     ResultSet resultSet,
                                                     Logger traceLog,
                                                     boolean failOnError,
                                                     Database database,
                                                     Table table ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (table == null) {
            throw new IllegalArgumentException("table");
        }

        // get catalog name
        String catalogName = (table.getCatalog() == null) ? null : table.getCatalog().getName();
        // get schema name
        String schemaName = (table.getSchema() == null) ? null : table.getSchema().getName();
        // get table name
        String tableName = table.getName();

        // column name
        String columnName = getString(resultSet, "COLUMN_NAME", false);
        // data type
        Integer dataType = getInteger(resultSet, "DATA_TYPE", false);
        // type name
        String typeName = getString(resultSet, "TYPE_NAME", false);
        // size
        Integer size = getInteger(resultSet, "COLUMN_SIZE", false);
        // column length in bytes
        // Integer bufferLength = getInteger(resultSet, "BUFFER_LENGTH", false);
        // precision
        Integer precision = getInteger(resultSet, "DECIMAL_DIGITS", false);

        // pseudo Column Type
        Integer columnPseudoType = getInteger(resultSet, "PSEUDO_COLUMN", false);

        // find table column object
        TableColumn column = table.findColumnByName(columnName);

        // create new
        if (column == null) {
            // creating column if not found (it is pseudo)
            column = factory.createTableColumn();

            // ***************************************
            // *** DatabaseNamedObject properties ***
            // ***************************************

            // name
            column.setName(columnName);

            // ***************
            // *** Column ***
            // ***************

            // owner
            column.setOwner(table);
            // SQL type
            column.setSqlType(getSqlType(dataType));
            // type name
            column.setTypeName(typeName);
            // Size
            column.setSize(size);
            // precision
            column.setPrecision(precision);
            // OrdinalPosition
            column.setOrdinalPosition(null); // N/A

            // ********************
            // *** Table Column ***
            // ********************

            // add column to the table
            table.addColumn(column);
        }

        // set pseudo type
        column.setPseudoType(getColumnPseudoType(columnPseudoType));

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) column %s has been updated or added.",
                                         database.getName(),
                                         tableName,
                                         schemaName,
                                         catalogName,
                                         columnName));
        }

        return column;
    }

    /**
     * Creates table's privileges based on ALL records in result set;
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param table the owner table
     * @return created list of privileges
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static Set<Privilege> populateTablePrivileges( ModelFactory factory,
                                                          ResultSet resultSet,
                                                          Logger traceLog,
                                                          boolean failOnError,
                                                          Database database,
                                                          Table table ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (table == null) {
            throw new IllegalArgumentException("table");
        }
        // get catalog name
        String catalogName = (table.getCatalog() == null) ? null : table.getCatalog().getName();
        // get schema name
        String schemaName = (table.getSchema() == null) ? null : table.getSchema().getName();
        // get table name
        String tableName = table.getName();

        // process all privileges
        while (resultSet.next()) {
            // grantor of access (may be null)
            String grantor = getString(resultSet, "GRANTOR", false);
            // grantee of access
            String grantee = getString(resultSet, "GRANTEE", false);
            // name of access (SELECT, INSERT, UPDATE, REFRENCES, ...)
            String privilegeName = getString(resultSet, "PRIVILEGE", false);
            // grantable string; "YES" if grantee is permitted to grant to others; "NO" if not; null if unknown
            String isGrantableStr = getString(resultSet, "IS_GRANTABLE", false);

            // create table's privilege
            Privilege privilege = factory.createPrivilege();
            // set name
            privilege.setName(privilegeName);
            // set PrivilegeType
            privilege.setPrivilegeType(getPrivilegeType(privilegeName));
            // set Grantor
            privilege.setGrantor(grantor);
            // set Grantee
            privilege.setGrantee(grantee);
            // set Grantable
            privilege.setGrantable("YES".equals(isGrantableStr) == true ? Boolean.TRUE : ("NO".equals(isGrantableStr) == true ? Boolean.FALSE : null));

            // add privilege
            table.addPrivilege(privilege);
            // log
            if (traceLog.isDebugEnabled()) {
                traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) privilege %s has been added.",
                                             database.getName(),
                                             tableName,
                                             schemaName,
                                             catalogName,
                                             privilegeName));
            }
        }

        // return
        return table.getPrivileges();
    }

    /**
     * Creates table's column privileges based on ALL records in result set;
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param table the owner table
     * @param column the table column
     * @return created list of privileges
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static Set<Privilege> populateTableColumnPrivileges( ModelFactory factory,
                                                                ResultSet resultSet,
                                                                Logger traceLog,
                                                                boolean failOnError,
                                                                Database database,
                                                                Table table,
                                                                TableColumn column ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (table == null) {
            throw new IllegalArgumentException("table");
        }

        // check for null
        if (column == null) {
            throw new IllegalArgumentException("column");
        }

        // get catalog name
        String catalogName = (table.getCatalog() == null) ? null : table.getCatalog().getName();
        // get schema name
        String schemaName = (table.getSchema() == null) ? null : table.getSchema().getName();
        // get table name
        String tableName = table.getName();

        // process all privileges
        while (resultSet.next()) {
            // grantor of access (may be null)
            String grantor = getString(resultSet, "GRANTOR", false);
            // grantee of access
            String grantee = getString(resultSet, "GRANTEE", false);
            // name of access (SELECT, INSERT, UPDATE, REFRENCES, ...)
            String privilegeName = getString(resultSet, "PRIVILEGE", false);
            // grantable string; "YES" if grantee is permitted to grant to others; "NO" if not; null if unknown
            String isGrantableStr = getString(resultSet, "IS_GRANTABLE", false);

            // create table's privilege
            Privilege privilege = factory.createPrivilege();
            // set name
            privilege.setName(privilegeName);
            // set PrivilegeType
            privilege.setPrivilegeType(getPrivilegeType(privilegeName));
            // set Grantor
            privilege.setGrantor(grantor);
            // set Grantee
            privilege.setGrantee(grantee);
            // set Grantable
            privilege.setGrantable("YES".equals(isGrantableStr) == true ? Boolean.TRUE : ("NO".equals(isGrantableStr) == true ? Boolean.FALSE : null));

            // add privilege
            column.addPrivilege(privilege);
            // log
            if (traceLog.isDebugEnabled()) {
                traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) column %s privilege %s has been added.",
                                             database.getName(),
                                             tableName,
                                             schemaName,
                                             catalogName,
                                             column.getName(),
                                             privilegeName));
            }
        }

        // return
        return column.getPrivileges();
    }

    /**
     * Creates SQL type info based on current record in result set; adds SQL type info to the database
     * 
     * @param factory the model factory to create table
     * @param resultSet the table result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @return created SQL type info
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static SqlTypeInfo populateSqlTypeInfo( ModelFactory factory,
                                                   ResultSet resultSet,
                                                   Logger traceLog,
                                                   boolean failOnError,
                                                   Database database ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // get type name
        String typeName = getString(resultSet, "TYPE_NAME", false);
        // data type
        Integer dataType = getInteger(resultSet, "DATA_TYPE", false);
        // precision
        Long precision = getLong(resultSet, "PRECISION", false);
        // literal prefix
        String literalPrefix = getString(resultSet, "LITERAL_PREFIX", false);
        // literal suffix
        String literalSuffix = getString(resultSet, "LITERAL_SUFFIX", false);
        // create params
        String createParams = getString(resultSet, "CREATE_PARAMS", false);
        // nullable
        Integer nullableType = getInteger(resultSet, "NULLABLE", false);
        // case sensitive
        Boolean caseSensitive = getBoolean(resultSet, "CASE_SENSITIVE", false);
        // searchable
        Integer searchableType = getInteger(resultSet, "SEARCHABLE", false);
        // is it unsigned
        Boolean unsignedAttribute = getBoolean(resultSet, "UNSIGNED_ATTRIBUTE", false);
        // is it fixed precision scale (can it be a money value)
        Boolean fixedPrecisionScale = getBoolean(resultSet, "FIXED_PREC_SCALE", false);
        // can it be used for an auto-increment value
        Boolean autoIncrement = getBoolean(resultSet, "AUTO_INCREMENT", false);
        // get local type name
        String localTypeName = getString(resultSet, "LOCAL_TYPE_NAME", false);
        // min scale supported
        Integer minScale = getInteger(resultSet, "MINIMUM_SCALE", false);
        // max scale supported
        Integer maxScale = getInteger(resultSet, "MAXIMUM_SCALE", false);
        // radix
        Integer radix = getInteger(resultSet, "NUM_PREC_RADIX", false);

        // create SQL type info object
        SqlTypeInfo typeInfo = factory.createSqlTypeInfo();

        // ***************************
        // *** DatabaseNamedObject ***
        // ***************************

        // set name
        typeInfo.setName(typeName);
        // set remarks
        // typeInfo.setRemarks (remarks); // N/A
        // TODO set extra properties
        // typeInfo.addExtraProperty (String key, Object value);

        // *******************
        // *** SqlTypeInfo ***
        // *******************

        // Localized type name
        typeInfo.setLocalizedTypeName(localTypeName);
        // SQL type nullability
        typeInfo.setNullabilityType(getNullabilityType(nullableType));
        // SQL type from java.sql.Types
        typeInfo.setSqlType(getSqlType(dataType));
        // precision
        typeInfo.setPrecision(precision);
        // fixed precision scale
        typeInfo.setFixedPrecisionScale(fixedPrecisionScale);
        // number precision radix
        typeInfo.setNumberPrecisionRadix(radix);
        // minimum scale supported
        typeInfo.setMinScale(minScale);
        // maximum scale supported
        typeInfo.setMaxScale(maxScale);
        // literal prefix
        typeInfo.setLiteralPrefix(literalPrefix);
        // literal suffix
        typeInfo.setLiteralSuffix(literalSuffix);
        // parameters used in creating the type (may be null)
        typeInfo.setCreateParams(createParams);
        // Case Sensitive
        typeInfo.setCaseSensitive(caseSensitive);
        // searchability type
        typeInfo.setSearchabilityType(getSearchabilityType(searchableType));
        // Unsigned
        typeInfo.setUnsigned(unsignedAttribute);
        // Auto Increment
        typeInfo.setAutoIncrement(autoIncrement);

        // add SQL type info to the list
        database.addSqlTypeInfo(typeInfo);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The SQL type '%s' has been added.", database.getName(), typeName));
        }

        return typeInfo;
    }

    /**
     * Creates UDT based on current record in result set; adds UDT to the database
     * 
     * @param factory the model factory to create table
     * @param resultSet the table result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @return created UDT
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static UserDefinedType populateUDT( ModelFactory factory,
                                               ResultSet resultSet,
                                               Logger traceLog,
                                               boolean failOnError,
                                               Database database ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // get type catalog
        String udtCatalog = getString(resultSet, "TYPE_CAT", false);
        // get type schema
        String udtSchema = getString(resultSet, "TYPE_SCHEM", false);
        // get type name
        String udtName = getString(resultSet, "TYPE_NAME", false);
        // get class name
        String className = getString(resultSet, "CLASS_NAME", false);
        // get data type
        Integer dataType = getInteger(resultSet, "DATA_TYPE", false);
        // get remarks
        String remarks = getString(resultSet, "REMARKS", false);
        // get base type
        Integer baseType = getInteger(resultSet, "BASE_TYPE", false);

        // create UDT object
        UserDefinedType udt = factory.createUserDefinedType();

        // ***************************
        // *** DatabaseNamedObject ***
        // ***************************

        // set name
        udt.setName(udtName);
        // set remarks
        udt.setRemarks(remarks);
        // TODO set extra properties
        // udt.addExtraProperty (String key, Object value);

        // ********************
        // *** SchemaObject ***
        // ********************

        // set catalog
        if ((udtCatalog != null) && (udtCatalog.trim().length() != 0)) {
            // find catalog
            Catalog catalog = database.findCatalogByName(udtCatalog);
            // set catalog
            udt.setCatalog(catalog);

            String errMessage = null;
            // warn if null
            if (catalog == null) {
                errMessage = String.format("[Database %s] Unable to find catalog '%4$s' for the UDT %s (schema %s, catalog %s)",
                                           database.getName(),
                                           udtName,
                                           udtSchema,
                                           udtCatalog);
                traceLog.debug(errMessage);
            }
            // if fail is enabled
            if (failOnError) {
                throw new DatabaseMetaDataMethodException(errMessage, "populateUDT");
            }
        }

        // set schema
        if ((udtSchema != null) && (udtSchema.trim().length() != 0)) {
            // find schema
            Schema schema = database.findSchemaByName(udtCatalog, udtSchema);
            // set schema
            udt.setSchema(schema);

            String errMessage = null;
            // warn if null
            if (schema == null) {
                errMessage = String.format("[Database %s] Unable to find schema '%3$s' for the UDT %s (schema %s, catalog %s)",
                                           database.getName(),
                                           udtName,
                                           udtSchema,
                                           udtCatalog);
                traceLog.debug(errMessage);
            }
            // if fail is enabled
            if (failOnError) {
                throw new DatabaseMetaDataMethodException(errMessage, "populateUTD");

            }
        }

        // **************
        // *** UDT ***
        // **************

        // class name
        udt.setClassName(className);
        // SQL type
        udt.setSqlType(getSqlType(dataType));
        // base type
        udt.setBaseType(getSqlType(baseType));

        // add UDT to the list
        database.addUserDefinedType(udt);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The UDT '%s' (schema %s, catalog %s) has been added.",
                                         database.getName(),
                                         udtName,
                                         udtSchema,
                                         udtCatalog));
        }

        return udt;
    }

    /**
     * Creates UDT attribute based on current record in result set; adds attribute to the UDT
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param udt the owner UDT
     * @return created UDT attribute
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static Attribute populateUDTAttribute( ModelFactory factory,
                                                  ResultSet resultSet,
                                                  Logger traceLog,
                                                  boolean failOnError,
                                                  Database database,
                                                  UserDefinedType udt ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (udt == null) {
            throw new IllegalArgumentException("udt");
        }
        // get catalog name
        String catalogName = (udt.getCatalog() == null) ? null : udt.getCatalog().getName();
        // get schema name
        String schemaName = (udt.getSchema() == null) ? null : udt.getSchema().getName();
        // get UDT name
        String udtName = udt.getName();

        // column name
        String columnName = getString(resultSet, "ATTR_NAME", false);
        // data type
        Integer dataType = getInteger(resultSet, "DATA_TYPE", false);
        // type name
        String typeName = getString(resultSet, "ATTR_TYPE_NAME", false);
        // size
        Integer size = getInteger(resultSet, "ATTR_SIZE", false);
        // precision
        Integer precision = getInteger(resultSet, "DECIMAL_DIGITS", false);
        // radix
        Integer radix = getInteger(resultSet, "NUM_PREC_RADIX", false);
        // nullable
        Integer nullableType = getInteger(resultSet, "NULLABLE", false);
        // remarks
        String remarks = getString(resultSet, "REMARKS", false);
        // default value
        String defaultValue = getString(resultSet, "ATTR_DEF", false);
        // character Octet Length
        Integer charOctetLength = getInteger(resultSet, "CHAR_OCTET_LENGTH", false);
        // ordinal position
        Integer ordinalPosition = getInteger(resultSet, "ORDINAL_POSITION", false);
        // is nullable string
        // String isNullableString = getString(resultSet, "IS_NULLABLE", false);
        // scope catalog
        String scopeCatalog = getString(resultSet, "SCOPE_CATLOG", false);
        // scope schema
        String scopeSchema = getString(resultSet, "SCOPE_SCHEMA", false);
        // scope table
        String scopeTable = getString(resultSet, "SCOPE_TABLE", false);
        // sourceDataType
        Integer sourceDataType = getInteger(resultSet, "SOURCE_DATA_TYPE", false);

        // create UDT attribute object
        Attribute column = factory.createAttribute();

        // ***************************************
        // *** DatabaseNamedObject properties ***
        // ***************************************

        // name
        column.setName(columnName);
        // remarks
        column.setRemarks(remarks);
        // TODO set extra properties
        // column.addExtraProperty (String key, Object value);

        // ***************
        // *** Column ***
        // ***************

        // owner
        column.setOwner(udt);
        // nullability. The isNullableString is not used so far
        column.setNullabilityType(getNullabilityType(nullableType));
        // SQL type
        column.setSqlType(getSqlType(dataType));
        // type name
        column.setTypeName(typeName);
        // Size
        column.setSize(size);
        // precision
        column.setPrecision(precision);
        // Radix
        column.setRadix(radix);
        // DefaultValue
        column.setDefaultValue(defaultValue);
        // OrdinalPosition
        column.setOrdinalPosition(ordinalPosition);
        // CharOctetLength
        column.setCharOctetLength(charOctetLength);
        // addPrivilege
        // column.addPrivilege (privilege); //

        // ********************
        // *** Attribute ***
        // ********************

        // set reference
        if ((scopeCatalog != null) || (scopeSchema != null) || (scopeTable != null) || (sourceDataType != null)) {
            // create reference
            Reference reference = factory.createReference();
            // set Source Data Type
            reference.setSourceDataType(getSqlType(sourceDataType));
            // find table and set as source
            reference.setSourceTable(database.findTableByName(scopeCatalog, scopeSchema, scopeTable));

            // set reference
            column.setReference(reference);
        }

        // add attribute to the UDT
        udt.addAttribute(column);

        // log
        if (traceLog.isDebugEnabled()) {
            traceLog.debug(String.format("[Database %s] The UDT '%s' (schema %s, catalog %s) attribute %s has been added.",
                                         database.getName(),
                                         udtName,
                                         schemaName,
                                         catalogName,
                                         columnName));
        }

        return column;
    }

    /**
     * Updates UDT super type info based on current record in result set
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param udt the UDT to update
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static void updateUDTSuperType( ModelFactory factory,
                                           ResultSet resultSet,
                                           Logger traceLog,
                                           boolean failOnError,
                                           Database database,
                                           UserDefinedType udt ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (udt == null) {
            throw new IllegalArgumentException("udt");
        }
        // get catalog name
        String catalogName = (udt.getCatalog() == null) ? null : udt.getCatalog().getName();
        // get schema name
        String schemaName = (udt.getSchema() == null) ? null : udt.getSchema().getName();
        // get UDT name
        String udtName = udt.getName();

        // super type catalog
        String superTypeCatalog = getString(resultSet, "SUPERTYPE_CAT", false);
        // super type schema
        String superTypeSchema = getString(resultSet, "SUPERTYPE_SCHEM", false);
        // super type
        String superTypeName = getString(resultSet, "SUPERTYPE_NAME", false);

        // ***********************
        // *** UserDefinedType ***
        // ***********************

        // set super type if found in database
        udt.setSuperType(database.findUserDefinedTypeByName(superTypeCatalog, superTypeSchema, superTypeName));

        // log
        if (udt.getSuperType() != null) {
            traceLog.debug(String.format("[Database %s] The UDT '%s' (schema %s, catalog %s) has super type %s (schema %s, catalog %s).",
                                         database.getName(),
                                         udtName,
                                         schemaName,
                                         catalogName,
                                         superTypeName,
                                         superTypeSchema,
                                         superTypeCatalog));
        }
    }

    /**
     * Updates table super table info based on current record in result set
     * 
     * @param factory the model factory to create SP parameter
     * @param resultSet the stored procedure parameter result set from DatabaseMetadata
     * @param traceLog the log to write if any
     * @param failOnError
     * @param database the owner database
     * @param table the table to update
     * @throws Exception if any error occurs and failOnError is true then generates exception
     */
    public static void updateTableSuperTable( ModelFactory factory,
                                              ResultSet resultSet,
                                              Logger traceLog,
                                              boolean failOnError,
                                              Database database,
                                              Table table ) throws Exception {
        // check for null
        if (factory == null) {
            throw new IllegalArgumentException("factory");
        }

        // check for null
        if (database == null) {
            throw new IllegalArgumentException("database");
        }

        // check for null
        if (resultSet == null) {
            throw new IllegalArgumentException("resultSet");
        }

        // set trace log
        if (traceLog == null) {
            traceLog = log;
        }

        // check for null
        if (table == null) {
            throw new IllegalArgumentException("table");
        }
        // get catalog name
        String catalogName = (table.getCatalog() == null) ? null : table.getCatalog().getName();
        // get schema name
        String schemaName = (table.getSchema() == null) ? null : table.getSchema().getName();
        // get table name
        String tableName = table.getName();

        // super table name
        String superTableName = getString(resultSet, "SUPERTABLE_NAME", false);

        // *************
        // *** Table ***
        // *************

        // set super table if found in database
        table.setSuperTable(database.findTableByName(catalogName, schemaName, tableName));

        // log
        if (table.getSuperTable() != null) {
            traceLog.debug(String.format("[Database %s] The table '%s' (schema %s, catalog %s) has super table %s.",
                                         database.getName(),
                                         tableName,
                                         schemaName,
                                         catalogName,
                                         superTableName));
        }
    }
    
    /**
     * Get simple database metadata for the getter method (no input parameters)
     * @param <T> the return type 
     * @param instance the instance of database metadata implementation
     * @param methodName the full name of a getter method to execute
     * @param traceLog the log
     * @return simple database metadata for the getter method
     */
    @SuppressWarnings("unchecked")
    public static <T> T getDatabaseMetadataProperty (DatabaseMetaData instance, String methodName, Logger traceLog) {
        try {
          // acces to the instance's RTTI  
          Method m = instance.getClass().getDeclaredMethod (methodName);
          // trying to execute method without parameters
          return (T) m.invoke(instance);
        } catch (Exception e) {
           traceLog.debug(String.format ("Unable to execute getDatabaseMetadata for the '%1$s' method - %2$s: %3$s", 
                                         methodName, e.getClass().getName(), e.getMessage()));
           // default is null
           return null;
        }
    } 
}
