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
 * Container for foreign key metadata. The fields in this class roughly parallel the information returned from the
 * {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)} method.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Immutable
public class ForeignKeyMetadata {

    private final String primaryKeyCatalogName;
    private final String primaryKeySchemaName;
    private final String primaryKeyTableName;
    private final String primaryKeyColumnName;
    private final String foreignKeyCatalogName;
    private final String foreignKeySchemaName;
    private final String foreignKeyTableName;
    private final String foreignKeyColumnName;
    private final int sequenceNr;
    private final int updateRule;
    private final int deleteRule;
    private final String foreignKeyName;
    private final String primaryKeyName;
    private final int deferrability;

    protected ForeignKeyMetadata( String primaryKeyCatalogName,
                                  String primaryKeySchemaName,
                                  String primaryKeyTableName,
                                  String primaryKeyColumnName,
                                  String foreignKeyCatalogName,
                                  String foreignKeySchemaName,
                                  String foreignKeyTableName,
                                  String foreignKeyColumnName,
                                  int sequenceNr,
                                  int updateRule,
                                  int deleteRule,
                                  String foreignKeyName,
                                  String primaryKeyName,
                                  int deferrability ) {
        this.primaryKeyCatalogName = primaryKeyCatalogName;
        this.primaryKeySchemaName = primaryKeySchemaName;
        this.primaryKeyTableName = primaryKeyTableName;
        this.primaryKeyColumnName = primaryKeyColumnName;
        this.foreignKeyCatalogName = foreignKeyCatalogName;
        this.foreignKeySchemaName = foreignKeySchemaName;
        this.foreignKeyTableName = foreignKeyTableName;
        this.foreignKeyColumnName = foreignKeyColumnName;
        this.sequenceNr = sequenceNr;
        this.updateRule = updateRule;
        this.deleteRule = deleteRule;
        this.foreignKeyName = foreignKeyName;
        this.primaryKeyName = primaryKeyName;
        this.deferrability = deferrability;
    }

    /**
     * @return the primary key catalog name (PKTABLE_CAT in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getPrimaryKeyCatalogName() {
        return primaryKeyCatalogName;
    }

    /**
     * @return the primary key schema name (PKTABLE_SCHEM in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getPrimaryKeySchemaName() {
        return primaryKeySchemaName;
    }

    /**
     * @return the primary key table name (PKTABLE_NAME in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getPrimaryKeyTableName() {
        return primaryKeyTableName;
    }

    /**
     * @return the primary key column name (PKCOLUMN_NAME in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getPrimaryKeyColumnName() {
        return primaryKeyColumnName;
    }

    /**
     * @return the foreign key catalog name (FKTABLE_CAT in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getForeignKeyCatalogName() {
        return foreignKeyCatalogName;
    }

    /**
     * @return the foreign key schema name (FKTABLE_SCHEM in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getForeignKeySchemaName() {
        return foreignKeySchemaName;
    }

    /**
     * @return the foreign key table name (FKTABLE_NAME in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getForeignKeyTableName() {
        return foreignKeyTableName;
    }

    /**
     * @return the foreign key column name (FKCOLUMN_NAME in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getForeignKeyColumnName() {
        return foreignKeyColumnName;
    }

    /**
     * @return the foreign key sequence number (KEY_SEQ in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public int getSequenceNr() {
        return sequenceNr;
    }

    /**
     * @return the foreign key update rule (UPDATE_RULE in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public int getUpdateRule() {
        return updateRule;
    }

    /**
     * @return the foreign key delete rule (DELETE_RULE in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public int getDeleteRule() {
        return deleteRule;
    }

    /**
     * @return the foreign key name (FK_NAME in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getForeignKeyName() {
        return foreignKeyName;
    }

    /**
     * @return the primary key name (PK_NAME in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public String getPrimaryKeyName() {
        return primaryKeyName;
    }

    /**
     * @return the deferrability of the foreign key constraint evaluation (DEFERRABILITY in the {@link java.sql.DatabaseMetaData#getExportedKeys(String, String, String)}
     *         result set).
     */
    public int getDeferrability() {
        return deferrability;
    }
}
