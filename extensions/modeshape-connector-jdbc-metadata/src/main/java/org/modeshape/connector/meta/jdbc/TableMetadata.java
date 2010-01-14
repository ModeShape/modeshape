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
package org.modeshape.connector.meta.jdbc;

import java.sql.DatabaseMetaData;
import net.jcip.annotations.Immutable;

/**
 * Container for table-level metadata. The fields in this class roughly parallel the information returned from the
 * {@link DatabaseMetaData#getTables(String, String, String, String[])} method.
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

    public TableMetadata( String name,
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
     * @return the table name (TABLE_NAME in the {@link DatabaseMetaData#getTables(String, String, String, String[])} result set).
     */
    public String getName() {
        return name;
    }

    /**
     * @return the table type (TABLE_TYPE in the {@link DatabaseMetaData#getTables(String, String, String, String[])} result set).
     */
    public String getType() {
        return type;
    }

    /**
     * @return the table description (REMARKS in the {@link DatabaseMetaData#getTables(String, String, String, String[])} result
     *         set).
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the table types catalog name (TYPE_CAT in the {@link DatabaseMetaData#getTables(String, String, String, String[])}
     *         result set).
     */
    public String getTypeCatalogName() {
        return typeCatalogName;
    }

    /**
     * @return the table types schema name (TYPE_SCHEM in the {@link DatabaseMetaData#getTables(String, String, String, String[])}
     *         result set).
     */
    public String getTypeSchemaName() {
        return typeSchemaName;
    }

    /**
     * @return the table type name (TYPE_NAME in the {@link DatabaseMetaData#getTables(String, String, String, String[])} result
     *         set).
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return per the Javadoc for the DatabaseMetaData method, "the name of the designated 'identifier' column of a typed table"
     *         (SELF_REFERENCING_COL_NAME in the {@link DatabaseMetaData#getTables(String, String, String, String[])} result set).
     */
    public String getSelfReferencingColumnName() {
        return selfReferencingColumnName;
    }

    /**
     * @return the strategy for creating the values in the self-referencing column (REF_GENERATION in the
     *         {@link DatabaseMetaData#getTables(String, String, String, String[])} result set).
     */
    public String getReferenceGenerationStrategyName() {
        return referenceGenerationStrategyName;
    }

}
