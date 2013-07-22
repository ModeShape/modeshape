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
