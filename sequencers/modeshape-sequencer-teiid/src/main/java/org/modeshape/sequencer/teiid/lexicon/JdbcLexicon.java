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
package org.modeshape.sequencer.teiid.lexicon;

/**
 * Constants associated with the JDBC namespace used in reading XMI models and writing JCR nodes.
 */
public interface JdbcLexicon {

    /**
     * The URI constant of the diagram namespace. The model and JRC namespace prefix are different.
     */
    public interface Namespace {
        String URI = "http://www.metamatrix.com/metamodels/JDBC";
    }

    /**
     * Constants associated with the JDBC namespace that identify XMI model identifiers.
     */
    public interface ModelId {
        String NS_PREFIX = "jdbc";
        String CONVERT_CASE_IN_MODEL = "convertCaseInModel";
        String CREATE_CATALOGS_IN_MODEL = "createCatalogsInModel";
        String CREATE_SCHEMAS_IN_MODEL = "createSchemasInModel";
        String DRIVER_CLASS = "driverClass";
        String DRIVER_NAME = "driverName";
        String EXCLUDED_OBJECT_PATHS = "excludedObjectPaths";
        String GENERATE_SOURCE_NAMES_IN_MODEL = "generateSourceNamesInModel";
        String SOURCE = "JdbcSource";
        String IMPORT_SETTINGS = "importSettings";
        String INCLUDE_APPROXIMATE_INDEXES = "includeApproximateIndexes";
        String INCLUDE_FOREIGN_KEYS = "includeForeignKeys";
        String INCLUDE_INDEXES = "includeIndexes";
        String INCLUDE_PROCEDURES = "includeProcedures";
        String INCLUDE_UNIQUE_INDEXES = "includeUniqueIndexes";
        String INCLUDED_CATALOG_PATHS = "includedCatalogPaths";
        String INCLUDED_SCHEMA_PATHS = "includedSchemaPaths";
        String INCLUDED_TABLE_TYPES = "includedTableTypes";
        String URL = "url";
        String USER_NAME = "username";
    }

    /**
     * JCR identifiers relating to the JDBC namespace.
     */
    public interface JcrId {
        String NS_PREFIX = "jdbcs";
        String CONVERT_CASE_IN_MODEL = NS_PREFIX + ':' + ModelId.CONVERT_CASE_IN_MODEL;
        String CREATE_CATALOGS_IN_MODEL = NS_PREFIX + ':' + ModelId.CREATE_CATALOGS_IN_MODEL;
        String CREATE_SCHEMAS_IN_MODEL = NS_PREFIX + ':' + ModelId.CREATE_SCHEMAS_IN_MODEL;
        String DRIVER_CLASS = NS_PREFIX + ':' + ModelId.DRIVER_CLASS;
        String DRIVER_NAME = NS_PREFIX + ':' + ModelId.DRIVER_NAME;
        String EXCLUDED_OBJECT_PATHS = NS_PREFIX + ':' + ModelId.EXCLUDED_OBJECT_PATHS;
        String GENERATE_SOURCE_NAMES_IN_MODEL = NS_PREFIX + ':' + ModelId.GENERATE_SOURCE_NAMES_IN_MODEL;
        String IMPORTED = NS_PREFIX + ":imported";
        String INCLUDE_APPROXIMATE_INDEXES = NS_PREFIX + ':' + ModelId.INCLUDE_APPROXIMATE_INDEXES;
        String INCLUDE_FOREIGN_KEYS = NS_PREFIX + ':' + ModelId.INCLUDE_FOREIGN_KEYS;
        String INCLUDE_INDEXES = NS_PREFIX + ':' + ModelId.INCLUDE_INDEXES;
        String INCLUDE_PROCEDURES = NS_PREFIX + ':' + ModelId.INCLUDE_PROCEDURES;
        String INCLUDE_UNIQUE_INDEXES = NS_PREFIX + ':' + ModelId.INCLUDE_UNIQUE_INDEXES;
        String INCLUDED_CATALOG_PATHS = NS_PREFIX + ':' + ModelId.INCLUDED_CATALOG_PATHS;
        String INCLUDED_SCHEMA_PATHS = NS_PREFIX + ':' + ModelId.INCLUDED_SCHEMA_PATHS;
        String INCLUDED_TABLE_TYPES = NS_PREFIX + ':' + ModelId.INCLUDED_TABLE_TYPES;
        String SOURCE = NS_PREFIX + ":source";
        String URL = NS_PREFIX + ':' + ModelId.URL;
        String USER_NAME = NS_PREFIX + ':' + ModelId.USER_NAME;
    }
}
