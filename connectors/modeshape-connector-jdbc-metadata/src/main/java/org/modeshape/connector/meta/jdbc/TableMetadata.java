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
 * Container for table-level metadata. The fields in this class roughly parallel the information returned from the
 * {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} method.
 */
@Immutable
public class TableMetadata {

    private final String name;
    private final String type;
    private final String description;
    private final String typeCatalogName;
    private final String typeSchemaName;
    private final String typeName;
    private final String selfReferencingColumnName;
    private final String referenceGenerationStrategyName;

    protected TableMetadata( String name,
                             String type,
                             String description,
                             String typeCatalogName,
                             String typeSchemaName,
                             String typeName,
                             String selfReferencingColumnName,
                             String referenceGenerationStrategyName ) {
        super();
        this.name = name;
        this.type = type;
        this.description = description;
        this.typeCatalogName = typeCatalogName;
        this.typeSchemaName = typeSchemaName;
        this.typeName = typeName;
        this.selfReferencingColumnName = selfReferencingColumnName;
        this.referenceGenerationStrategyName = referenceGenerationStrategyName;
    }

    /**
     * @return the table name (TABLE_NAME in the {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} result set).
     */
    public String getName() {
        return name;
    }

    /**
     * @return the table type (TABLE_TYPE in the {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} result set).
     */
    public String getType() {
        return type;
    }

    /**
     * @return the table description (REMARKS in the {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} result
     *         set).
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the table types catalog name (TYPE_CAT in the {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])}
     *         result set).
     */
    public String getTypeCatalogName() {
        return typeCatalogName;
    }

    /**
     * @return the table types schema name (TYPE_SCHEM in the {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])}
     *         result set).
     */
    public String getTypeSchemaName() {
        return typeSchemaName;
    }

    /**
     * @return the table type name (TYPE_NAME in the {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} result
     *         set).
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return per the Javadoc for the DatabaseMetaData method, "the name of the designated 'identifier' column of a typed table"
     *         (SELF_REFERENCING_COL_NAME in the {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} result set).
     */
    public String getSelfReferencingColumnName() {
        return selfReferencingColumnName;
    }

    /**
     * @return the strategy for creating the values in the self-referencing column (REF_GENERATION in the
     *         {@link java.sql.DatabaseMetaData#getTables(String, String, String, String[])} result set).
     */
    public String getReferenceGenerationStrategyName() {
        return referenceGenerationStrategyName;
    }

}
