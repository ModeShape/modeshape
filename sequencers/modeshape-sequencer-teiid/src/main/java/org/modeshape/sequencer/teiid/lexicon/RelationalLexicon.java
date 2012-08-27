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

import static org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.Namespace.PREFIX;

/**
 * Constants for use in reading XMI relational models and writing JCR nodes.
 */
public interface RelationalLexicon {

    /**
     * Constants relating to the the relational namespace.
     */
    public interface Namespace {
        String PREFIX = "relational";
        String URI = "http://www.metamatrix.com/metamodels/Relational";
    }

    /**
     * Constants used within an XMI relational model.
     */
    public interface ModelIds {
        String ACCESS_PATTERN = "accessPatterns";
        String AUTO_INCREMENTED = "autoIncremented";
        String BASE_TABLE = "BaseTable";
        String CASE_SENSITIVE = "caseSensitive";
        String CATALOG = "Catalog";
        String CHARACTER_SET_NAME = "characterSetName";
        String COLLATION_NAME = "collationName";
        String COLUMNS = "columns";
        String CURRENCY = "currency";
        String DEFAULT_VALUE = "defaultValue";
        String DISTINCT_VALUE_COUNT = "distinctValueCount";
        String FIXED_LENGTH = "fixedLength";
        String FOREIGN_KEY = "foreignKeys";
        String FORMAT = "format";
        String HREF = "href";
        String LENGTH = "length";
        String MAX_VALUE = "maximumValue";
        String MIN_VALUE = "minimumValue";
        String NAME = "name";
        String NAME_IN_SOURCE = "nameInSource";
        String NATIVE_TYPE = "nativeType";
        String NULL_VALUE_COUNT = "nullValueCount";
        String NULLABLE = "nullable";
        String PRECISION = "precision";
        String PRIMARY_KEY = "primaryKey";
        String PROCEDURE = "Procedure";
        String PROCEDURES = "procedures";
        String PROCEDURE_PARAMETER = "parameters";
        String PROCEDURE_RESULT = "result";
        String RADIX = "radix";
        String SCALE = "scale";
        String SEARCHABILITY = "searchability";
        String SELECTABLE = "selectable";
        String SCHEMA = "Schema";
        String SCHEMAS = "schemas";
        String SIGNED = "signed";
        String TABLE = "BaseTable";
        String TABLES = "tables";
        String TYPE = "type";
        String UPDATEABLE = "updateable";
        String VIEW = "view";
    }

    /**
     * JCR identifiers relating to relational namespace.
     */
    public interface JcrIds {
        String ACCESS_PATTERN = PREFIX + ":accessPattern";
        String ACCESS_PATTERN_HREFS = PREFIX + ":accessPatternHrefs";
        String ACCESS_PATTERN_NAMES = PREFIX + ":accessPatternNames";
        String ACCESS_PATTERN_XMI_UUIDS = PREFIX + ":accessPatternXmiUuids";
        String ACCESS_PATTERNS = PREFIX + ":accessPatterns";
        String AUTO_INCREMENTED = PREFIX + ':' + ModelIds.AUTO_INCREMENTED;
        String AUTO_UPDATE = PREFIX + ":autoUpdate";
        String BASE_TABLE = PREFIX + ":baseTable";
        String CARDINALITY = PREFIX + ":cardinality";
        String CASE_SENSITIVE = PREFIX + ':' + ModelIds.CASE_SENSITIVE;
        String CATALOG = PREFIX + ":catalog";
        String CHARACTER_SET_NAME = PREFIX + ':' + ModelIds.CHARACTER_SET_NAME;
        String COLLATION_NAME = PREFIX + ':' + ModelIds.COLLATION_NAME;
        String COLUMN = PREFIX + ":column";
        String COLUMN_NAMES = PREFIX + ":columnNames";
        String COLUMN_SET = PREFIX + ":columnSet";
        String COLUMN_XMI_UUIDS = PREFIX + ":columnXmiUuids";
        String COLUMNS = PREFIX + ':' + ModelIds.COLUMNS;
        String CURRENCY = PREFIX + ':' + ModelIds.CURRENCY;
        String DEFAULT_VALUE = PREFIX + ':' + ModelIds.DEFAULT_VALUE;
        String DIRECTION = PREFIX + ":direction";
        String DISTINCT_VALUE_COUNT = PREFIX + ':' + ModelIds.DISTINCT_VALUE_COUNT;
        String FILTER_CONDITION = PREFIX + ":filterCondition";
        String FIXED_LENGTH = PREFIX + ':' + ModelIds.FIXED_LENGTH;
        String FOREIGN_KEY = PREFIX + ":foreignKey";
        String FOREIGN_KEY_HREFS = PREFIX + ":foreignKeyHrefs";
        String FOREIGN_KEY_MULTIPLICITY = PREFIX + ":foreignKeyMultiplicity";
        String FOREIGN_KEY_NAMES = PREFIX + ":foreignKeyNames";
        String FOREIGN_KEY_XMI_UUIDS = PREFIX + ":foreignKeyXmiUuids";
        String FOREIGN_KEYS = PREFIX + ":foreignKeys";
        String FORMAT = PREFIX + ':' + ModelIds.FORMAT;
        String FUNCTION = PREFIX + ":function";
        String INDEX = PREFIX + ":index";
        String INDEX_HREFS = PREFIX + ":indexHrefs";
        String INDEX_NAMES = PREFIX + ":indexNames";
        String INDEX_XMI_UUIDS = PREFIX + ":indexXmiUuids";
        String INDEXES = PREFIX + ":indexes";
        String LENGTH = PREFIX + ':' + ModelIds.LENGTH;
        String LOGICAL_RELATIONSHIP = PREFIX + ":logicalRelationship";
        String LOGICAL_RELATIONSHIP_END = PREFIX + ":logicalRelationshipEnd";
        String LOGICAL_RELATIONSHIP_HREFS = PREFIX + ":logicalRelationshipHrefs";
        String LOGICAL_RELATIONSHIP_NAMES = PREFIX + ":logicalRelationshipNames";
        String LOGICAL_RELATIONSHIP_XMI_UUIDS = PREFIX + ":logicalRelationshipXmiUuids";
        String LOGICAL_RELATIONSHIPS = PREFIX + ":logicalRelationships";
        String MATERIALIZED = PREFIX + ":materialized";
        String MAX_VALUE = PREFIX + ':' + ModelIds.MAX_VALUE;
        String MIN_VALUE = PREFIX + ':' + ModelIds.MIN_VALUE;
        String MULTIPLICITY = PREFIX + ":multiplicity";
        String NAME_IN_SOURCE = PREFIX + ":nameInSource";
        String NATIVE_TYPE = PREFIX + ':' + ModelIds.NATIVE_TYPE;
        String NULL_VALUE_COUNT = PREFIX + ':' + ModelIds.NULL_VALUE_COUNT;
        String NULLABLE = PREFIX + ':' + ModelIds.NULLABLE;
        String PRECISION = PREFIX + ':' + ModelIds.PRECISION;
        String PRIMARY_KEY = PREFIX + ':' + ModelIds.PRIMARY_KEY;
        String PRIMARY_KEY_MULTIPLICITY = PREFIX + ":primaryKeyMultiplicity";
        String PROCEDURE = PREFIX + ":procedure";
        String PROCEDURE_PARAMETER = PREFIX + ":procedureParameter";
        String PROCEDURE_RESULT = PREFIX + ":procedureResult";
        String RADIX = PREFIX + ':' + ModelIds.RADIX;
        String RELAIONAL_ENTITY = PREFIX + ":relationalEntity";
        String RELATIONSHIP = PREFIX + ":relationship";
        String SCALE = PREFIX + ':' + ModelIds.SCALE;
        String SCHEMA = PREFIX + ":schema";
        String SEARCHABILITY = PREFIX + ':' + ModelIds.SEARCHABILITY;
        String SELECTABLE = PREFIX + ':' + ModelIds.SELECTABLE;
        String SIGNED = PREFIX + ':' + ModelIds.SIGNED;
        String SUPPORTS_UPDATE = PREFIX + ":supportsUpdate";
        String SYSTEM = PREFIX + ":system";
        String TABLE = PREFIX + ":table";
        String TABLE_HREF = PREFIX + ":tableHref";
        String TABLE_NAME = PREFIX + ":tableName";
        String TABLE_XMI_UUID = PREFIX + ":tableXmiUuid";
        String TYPE = PREFIX + ':' + ModelIds.TYPE;
        String TYPE_HREF = PREFIX + ":typeHref";
        String TYPE_NAME = PREFIX + ":typeName";
        String TYPE_XMI_UUID = PREFIX + ":typeXmiUuid";
        String UNIQUE = PREFIX + ":unique";
        String UNIQUE_CONSTRAINT = PREFIX + ":uniqueConstraint";
        String UNIQUE_KEY = PREFIX + ":uniqueKey";
        String UNIQUE_KEY_HREFS = PREFIX + ":uniqueKeyHrefs";
        String UNIQUE_KEY_NAMES = PREFIX + ":uniqueKeyNames";
        String UNIQUE_KEY_XMI_UUIDS = PREFIX + ":uniqueKeyXmiUuids";
        String UNIQUE_KEYS = PREFIX + ":uniqueKeys";
        String UPDATE_COUNT = PREFIX + ":updateCount";
        String UPDATEABLE = PREFIX + ':' + ModelIds.UPDATEABLE;
        String VIEW = PREFIX + ":view";
    }
}
