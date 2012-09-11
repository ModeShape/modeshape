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
