/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.common.database;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Utility for interacting with various databases.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class DatabaseUtil {

    private DatabaseUtil() {
    }

    /**
     * Determine the type of a database, based on the metadata information from the DB metadata.
     *
     * @param metaData a {@link DatabaseMetaData} instance, may not be null
     * @return a {@link DatabaseType} instance, never null
     * @throws SQLException if a database access error occurs
     * or this method is called on a closed connection
     */
    public static DatabaseType determineType(DatabaseMetaData metaData) throws SQLException {
        metaData = Objects.requireNonNull(metaData, "metaData cannot be null");
        int majorVersion = metaData.getDatabaseMajorVersion();
        int minorVersion = metaData.getDatabaseMinorVersion();
        String name = metaData.getDatabaseProductName().toLowerCase();
        if (name.contains("mysql")) {
            return new DatabaseType(DatabaseType.Name.MYSQL, majorVersion, minorVersion);
        } else if (name.contains("postgres")) {
            return new DatabaseType(DatabaseType.Name.POSTGRES, majorVersion, minorVersion);
        } else if (name.contains("derby")) {
            return new DatabaseType(DatabaseType.Name.DERBY, majorVersion, minorVersion);
        } else if (name.contains("hsql") || name.toLowerCase().contains("hypersonic")) {
            return new DatabaseType(DatabaseType.Name.HSQL, majorVersion, minorVersion);
        } else if (name.contains("h2")) {
            return new DatabaseType(DatabaseType.Name.H2, majorVersion, minorVersion);
        } else if (name.contains("sqlite")) {
            return new DatabaseType(DatabaseType.Name.SQLITE, majorVersion, minorVersion);
        } else if (name.contains("db2")) {
            return new DatabaseType(DatabaseType.Name.DB2, majorVersion, minorVersion);
        } else if (name.contains("informix")) {
            return new DatabaseType(DatabaseType.Name.INFORMIX, majorVersion, minorVersion);
        } else if (name.contains("interbase")) {
            return new DatabaseType(DatabaseType.Name.INTERBASE, majorVersion, minorVersion);
        } else if (name.contains("firebird")) {
            return new DatabaseType(DatabaseType.Name.FIREBIRD, majorVersion, minorVersion);
        } else if (name.contains("sqlserver") || name.toLowerCase().contains("microsoft")) {
            return new DatabaseType(DatabaseType.Name.SQLSERVER, majorVersion, minorVersion);
        } else if (name.contains("access")) {
            return new DatabaseType(DatabaseType.Name.ACCESS, majorVersion, minorVersion);
        } else if (name.contains("oracle")) {
            return new DatabaseType(DatabaseType.Name.ORACLE, majorVersion, minorVersion);
        } else if (name.contains("adaptive")) {
            return new DatabaseType(DatabaseType.Name.SYBASE, majorVersion, minorVersion);
        } else if (name.contains("cassandra")) {
            return new DatabaseType(DatabaseType.Name.CASSANDRA, majorVersion, minorVersion);
        }
        return DatabaseType.UNKNOWN;
    }
}
