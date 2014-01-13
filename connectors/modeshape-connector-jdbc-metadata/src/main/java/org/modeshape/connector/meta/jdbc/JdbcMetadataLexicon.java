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


import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.basic.BasicName;

/**
 * The namespace and property names used within a {@link JdbcMetadataConnector} to store internal information.
 */
public class JdbcMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/connector/meta/jdbc/1.0";
    }

    public static final Name CATALOG = new BasicName(Namespace.URI, "catalog");
    public static final Name COLUMN = new BasicName(Namespace.URI, "column");
    public static final Name COLUMN_SIZE = new BasicName(Namespace.URI, "columnSize");
    public static final Name DATABASE_MAJOR_VERSION = new BasicName(Namespace.URI, "databaseMajorVersion");
    public static final Name DATABASE_MINOR_VERSION = new BasicName(Namespace.URI, "databaseMinorVersion");
    public static final Name DATABASE_PRODUCT_NAME = new BasicName(Namespace.URI, "databaseProductName");
    public static final Name DATABASE_PRODUCT_VERSION = new BasicName(Namespace.URI, "databaseProductVersion");
    public static final Name DATABASE_ROOT = new BasicName(Namespace.URI, "databaseRoot"); // Mixin type for root node
    public static final Name DECIMAL_DIGITS = new BasicName(Namespace.URI, "decimalDigits");
    public static final Name DEFAULT_VALUE = new BasicName(Namespace.URI, "defaultValue");
    public static final Name DESCRIPTION = new BasicName(Namespace.URI, "description");
    public static final Name JDBC_DATA_TYPE = new BasicName(Namespace.URI, "jdbcDataType");
    public static final Name NULLABLE = new BasicName(Namespace.URI, "nullable");
    public static final Name LENGTH = new BasicName(Namespace.URI, "length");
    public static final Name ORDINAL_POSITION = new BasicName(Namespace.URI, "ordinalPosition");
    public static final Name PROCEDURE = new BasicName(Namespace.URI, "procedure");
    public static final Name PROCEDURES = new BasicName(Namespace.URI, "procedures");
    public static final Name PROCEDURE_RETURN_TYPE = new BasicName(Namespace.URI, "procedureReturnType");
    public static final Name RADIX = new BasicName(Namespace.URI, "radix");
    public static final Name REFERENCE_GENERATION_STRATEGY_NAME = new BasicName(Namespace.URI,
                                                                                "referenceGenerationStrategyName");
    public static final Name SCHEMA = new BasicName(Namespace.URI, "schema");
    public static final Name SCOPE_CATALOG_NAME = new BasicName(Namespace.URI, "scopeCatalogName");
    public static final Name SCOPE_SCHEMA_NAME = new BasicName(Namespace.URI, "scopeSchemaName");
    public static final Name SCOPE_TABLE_NAME = new BasicName(Namespace.URI, "scopeTableName");
    public static final Name SELF_REFERENCING_COLUMN_NAME = new BasicName(Namespace.URI, "selfReferencingColumnName");
    public static final Name SOURCE_JDBC_DATA_TYPE = new BasicName(Namespace.URI, "sourceJdbcDataType");
    public static final Name TABLE = new BasicName(Namespace.URI, "table");
    public static final Name TABLES = new BasicName(Namespace.URI, "tables");
    public static final Name TABLE_TYPE = new BasicName(Namespace.URI, "tableType");
    public static final Name TYPE_NAME = new BasicName(Namespace.URI, "typeName");
    public static final Name TYPE_CATALOG_NAME = new BasicName(Namespace.URI, "typeCatalogName");
    public static final Name TYPE_SCHEMA_NAME = new BasicName(Namespace.URI, "typeSchemaName");
    public static final Name FOREIGN_KEYS = new BasicName(Namespace.URI, "foreignKeys");
    public static final Name FOREIGN_KEY = new BasicName(Namespace.URI, "foreignKey");
    public static final Name PRIMARY_KEY_CATALOG_NAME = new BasicName(Namespace.URI, "primaryKeyCatalogName");
    public static final Name PRIMARY_KEY_SCHEMA_NAME = new BasicName(Namespace.URI, "primaryKeySchemaName");
    public static final Name PRIMARY_KEY_TABLE_NAME = new BasicName(Namespace.URI, "primaryKeyTableName");
    public static final Name PRIMARY_KEY_COLUMN_NAME = new BasicName(Namespace.URI, "primaryKeyColumnName");
    public static final Name FOREIGN_KEY_CATALOG_NAME = new BasicName(Namespace.URI, "foreignKeyCatalogName");
    public static final Name FOREIGN_KEY_SCHEMA_NAME = new BasicName(Namespace.URI, "foreignKeySchemaName");
    public static final Name FOREIGN_KEY_TABLE_NAME = new BasicName(Namespace.URI, "foreignKeyTableName");
    public static final Name FOREIGN_KEY_COLUMN_NAME = new BasicName(Namespace.URI, "foreignKeyColumnName");
    public static final Name SEQUENCE_NR = new BasicName(Namespace.URI, "sequenceNr");
    public static final Name UPDATE_RULE = new BasicName(Namespace.URI, "updateRule");
    public static final Name DELETE_RULE = new BasicName(Namespace.URI, "deleteRule");
    public static final Name PRIMARY_KEY_NAME = new BasicName(Namespace.URI, "foreignKeyName");
    public static final Name FOREIGN_KEY_NAME = new BasicName(Namespace.URI, "primaryKeyName");
    public static final Name DEFERRABILITY = new BasicName(Namespace.URI, "deferrability");
}
