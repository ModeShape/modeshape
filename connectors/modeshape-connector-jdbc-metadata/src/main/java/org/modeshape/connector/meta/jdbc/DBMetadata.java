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
package org.modeshape.connector.meta.jdbc;

import org.modeshape.common.annotation.Immutable;

/**
 * Container for database metadata. The fields in this class contain information obtained via the
 * {@code java.sql.DatabaseMetaData#getDatabase} methods.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Immutable
public class DBMetadata {

    private final String databaseProductName;
    private final String databaseProductVersion;
    private final int databaseMajorVersion;
    private final int databaseMinorVersion;

    protected DBMetadata( String databaseProductName,
                          String databaseProductVersion,
                          int databaseMajorVersion,
                          int databaseMinorVersion ) {
        this.databaseProductName = databaseProductName;
        this.databaseProductVersion = databaseProductVersion;
        this.databaseMajorVersion = databaseMajorVersion;
        this.databaseMinorVersion = databaseMinorVersion;
    }

    /**
     * @return the result of the {@link java.sql.DatabaseMetaData#getDatabaseProductName()}.
     */
    public String getDatabaseProductName() {
        return databaseProductName;
    }

    /**
     * @return the result of the {@link java.sql.DatabaseMetaData#getDatabaseProductVersion()}.
     */
    public String getDatabaseProductVersion() {
        return databaseProductVersion;
    }

    /**
     * @return the result of the {@link java.sql.DatabaseMetaData#getDatabaseMajorVersion()}.
     */
    public int getDatabaseMajorVersion() {
        return databaseMajorVersion;
    }

    /**
     * @return the result of the {@link java.sql.DatabaseMetaData#getDatabaseMinorVersion()}.
     */
    public int getDatabaseMinorVersion() {
        return databaseMinorVersion;
    }
}
