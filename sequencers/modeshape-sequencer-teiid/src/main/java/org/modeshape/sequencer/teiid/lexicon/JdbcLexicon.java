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
        String EXCLUDED_OBJECT_PATHS = "excludedObjectPaths";
        String SOURCE = "JdbcSource";
        String IMPORT_SETTINGS = "importSettings";
        String INCLUDED_CATALOG_PATHS = "includedCatalogPaths";
        String INCLUDED_SCHEMA_PATHS = "includedSchemaPaths";
        String INCLUDED_TABLE_TYPES = "includedTableTypes";
    }

    /**
     * JCR identifiers relating to the JDBC namespace.
     */
    public interface JcrId {
        String NS_PREFIX = "jdbcs";
        String CONVERT_CASE_IN_MODEL = NS_PREFIX + ":convertCaseInModel";
        String CREATE_CATALOGS_IN_MODEL = NS_PREFIX + ":createCatalogsInModel";
        String CREATE_SCHEMAS_IN_MODEL = NS_PREFIX + ":createSchemasInModel";
        String DRIVER_CLASS = NS_PREFIX + ":driverClass";
        String DRIVER_NAME = NS_PREFIX + ":driverName";
        String EXCLUDED_OBJECT_PATHS = NS_PREFIX + ':' + ModelId.EXCLUDED_OBJECT_PATHS;
        String GENERATE_SOURCE_NAMES_IN_MODEL = NS_PREFIX + ":generateSourceNamesInModel";
        String IMPORTED = NS_PREFIX + ":imported";
        String INCLUDE_APPROXIMATE_INDEXES = NS_PREFIX + ":includeApproximateIndexes";
        String INCLUDE_FOREIGN_KEYS = NS_PREFIX + ":includeForeignKeys";
        String INCLUDE_INDEXES = NS_PREFIX + ":includeIndexes";
        String INCLUDE_PROCEDURES = NS_PREFIX + ":includeProcedures";
        String INCLUDE_UNIQUE_INDEXES = NS_PREFIX + ":includeUniqueIndexes";
        String INCLUDED_CATALOG_PATHS = NS_PREFIX + ':' + ModelId.INCLUDED_CATALOG_PATHS;
        String INCLUDED_SCHEMA_PATHS = NS_PREFIX + ':' + ModelId.INCLUDED_SCHEMA_PATHS;
        String INCLUDED_TABLE_TYPES = NS_PREFIX + ':' + ModelId.INCLUDED_TABLE_TYPES;
        String SOURCE = NS_PREFIX + ":source";
        String URL = NS_PREFIX + ":url";
        String USER_NAME = NS_PREFIX + ":username";
    }
}
