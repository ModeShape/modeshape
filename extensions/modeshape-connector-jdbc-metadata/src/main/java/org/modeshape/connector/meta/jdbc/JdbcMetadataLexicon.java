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

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * The namespace and property names used within a {@link JdbcMetadataSource} to store internal information.
 */
public class JdbcMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/connector/meta/jdbc/1.0";
        public static final String PREFIX = "metajdbc";
    }

    public static final Name CATALOG = new BasicName(Namespace.URI, "catalog");
    public static final Name COLUMN = new BasicName(Namespace.URI, "column");
    public static final Name COLUMNS = new BasicName(Namespace.URI, "columns");
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
    public static final Name REFERENCE_GENERATION_STRATEGY_NAME = new BasicName(Namespace.URI, "referenceGenerationStrategyName");
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
}
