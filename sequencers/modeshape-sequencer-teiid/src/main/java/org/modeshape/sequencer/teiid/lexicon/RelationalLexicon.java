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

import static org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.Namespace.PREFIX;

/**
 * Constants associated with the relational namespace used in reading XMI models and writing JCR nodes.
 */
public interface RelationalLexicon {

    /**
     * The URI and prefix constants of the relational namespace.
     */
    public interface Namespace {
        String PREFIX = "relational";
        String URI = "http://www.metamatrix.com/metamodels/Relational";
    }

    /**
     * Constants associated with the relatonal namespace that identify XMI model identifiers.
     */
    public interface ModelId {
        String ACCESS_PATTERN = "AccessPattern";
        String ACCESS_PATTERNS = "accessPatterns";
        String AUTO_INCREMENTED = "autoIncremented";
        String AUTO_UPDATE = "autoUpdate";
        String BASE_TABLE = "BaseTable";
        String CARDINALITY = "cardinality";
        String CASE_SENSITIVE = "caseSensitive";
        String CATALOG = "Catalog";
        String CHARACTER_SET_NAME = "characterSetName";
        String COLLATION_NAME = "collationName";
        String COLUMNS = "columns";
        String CURRENCY = "currency";
        String DEFAULT_VALUE = "defaultValue";
        String DIRECTION = "direction";
        String DISTINCT_VALUE_COUNT = "distinctValueCount";
        String FILTER_CONDITION = "filterCondition";
        String FIXED_LENGTH = "fixedLength";
        String FOREIGN_KEYS = "foreignKeys";
        String FOREIGN_KEY_MULTIPLICITY = "foreignKeyMultiplicity";
        String FORMAT = "format";
        String FUNCTION = "function";
        String HREF = "href";
        String INDEX = "Index";
        String INDEXES = "indexes";
        String LENGTH = "length";
        String MATERIALIZED = "materialized";
        String MAX_VALUE = "maximumValue";
        String MIN_VALUE = "minimumValue";
        String NAME = "name";
        String NAME_IN_SOURCE = "nameInSource";
        String NATIVE_TYPE = "nativeType";
        String NULL_VALUE_COUNT = "nullValueCount";
        String NULLABLE = "nullable";
        String PRECISION = "precision";
        String PRIMARY_KEY = "primaryKey";
        String PRIMARY_KEY_MULTIPLICITY = "primaryKeyMultiplicity";
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
        String SUPPORTS_UPDATE = "supportsUpdate";
        String SYSTEM = "system";
        String TABLE = "BaseTable";
        String TABLES = "tables";
        String TABLES_VIEW = "View";
        String TYPE = "type";
        String UNIQUE = "unique";
        String UNIQUE_CONSTRAINT = "uniqueConstraints";
        String UNIQUE_KEYS = "uniqueKeys";
        String UPDATEABLE = "updateable";
        String UPDATE_COUNT = "updateCount";
        String VIEW = "view";
    }

    /**
     * JCR identifiers relating to the relational namespace.
     */
    public interface JcrId {
        String ACCESS_PATTERN = PREFIX + ":accessPattern";
        String ACCESS_PATTERN_HREFS = PREFIX + ":accessPatternHrefs";
        String ACCESS_PATTERN_NAMES = PREFIX + ":accessPatternNames";
        String ACCESS_PATTERN_XMI_UUIDS = PREFIX + ":accessPatternXmiUuids";
        String ACCESS_PATTERNS = PREFIX + ":accessPatterns";
        String AUTO_INCREMENTED = PREFIX + ':' + ModelId.AUTO_INCREMENTED;
        String AUTO_UPDATE = PREFIX + ':' + ModelId.AUTO_UPDATE;
        String BASE_TABLE = PREFIX + ":baseTable";
        String CARDINALITY = PREFIX + ':' + ModelId.CARDINALITY;
        String CASE_SENSITIVE = PREFIX + ':' + ModelId.CASE_SENSITIVE;
        String CATALOG = PREFIX + ":catalog";
        String CHARACTER_SET_NAME = PREFIX + ':' + ModelId.CHARACTER_SET_NAME;
        String COLLATION_NAME = PREFIX + ':' + ModelId.COLLATION_NAME;
        String COLUMN = PREFIX + ":column";
        String COLUMN_NAMES = PREFIX + ":columnNames";
        String COLUMN_SET = PREFIX + ":columnSet";
        String COLUMN_XMI_UUIDS = PREFIX + ":columnXmiUuids";
        String COLUMNS = PREFIX + ':' + ModelId.COLUMNS;
        String CURRENCY = PREFIX + ':' + ModelId.CURRENCY;
        String DEFAULT_VALUE = PREFIX + ':' + ModelId.DEFAULT_VALUE;
        String DIRECTION = PREFIX + ':' + ModelId.DIRECTION;
        String DISTINCT_VALUE_COUNT = PREFIX + ':' + ModelId.DISTINCT_VALUE_COUNT;
        String FILTER_CONDITION = PREFIX + ':' + ModelId.FILTER_CONDITION;
        String FIXED_LENGTH = PREFIX + ':' + ModelId.FIXED_LENGTH;
        String FOREIGN_KEY = PREFIX + ":foreignKey";
        String FOREIGN_KEY_HREFS = PREFIX + ":foreignKeyHrefs";
        String FOREIGN_KEY_MULTIPLICITY = PREFIX + ':' + ModelId.FOREIGN_KEY_MULTIPLICITY;
        String FOREIGN_KEY_NAMES = PREFIX + ":foreignKeyNames";
        String FOREIGN_KEY_XMI_UUIDS = PREFIX + ":foreignKeyXmiUuids";
        String FOREIGN_KEYS = PREFIX + ":foreignKeys";
        String FORMAT = PREFIX + ':' + ModelId.FORMAT;
        String FUNCTION = PREFIX + ':' + ModelId.FUNCTION;
        String INDEX = PREFIX + ":index";
        String INDEX_HREFS = PREFIX + ":indexHrefs";
        String INDEX_NAMES = PREFIX + ":indexNames";
        String INDEX_XMI_UUIDS = PREFIX + ":indexXmiUuids";
        String INDEXES = PREFIX + ":indexes";
        String LENGTH = PREFIX + ':' + ModelId.LENGTH;
        String MATERIALIZED = PREFIX + ':' + ModelId.MATERIALIZED;
        String MAX_VALUE = PREFIX + ':' + ModelId.MAX_VALUE;
        String MIN_VALUE = PREFIX + ':' + ModelId.MIN_VALUE;
        String MULTIPLICITY = PREFIX + ":multiplicity";
        String NAME_IN_SOURCE = PREFIX + ":nameInSource";
        String NATIVE_TYPE = PREFIX + ':' + ModelId.NATIVE_TYPE;
        String NULL_VALUE_COUNT = PREFIX + ':' + ModelId.NULL_VALUE_COUNT;
        String NULLABLE = PREFIX + ':' + ModelId.NULLABLE;
        String PRECISION = PREFIX + ':' + ModelId.PRECISION;
        String PRIMARY_KEY = PREFIX + ':' + ModelId.PRIMARY_KEY;
        String PRIMARY_KEY_MULTIPLICITY = PREFIX + ':' + ModelId.PRIMARY_KEY_MULTIPLICITY;
        String PROCEDURE = PREFIX + ":procedure";
        String PROCEDURE_PARAMETER = PREFIX + ":procedureParameter";
        String PROCEDURE_RESULT = PREFIX + ":procedureResult";
        String RADIX = PREFIX + ':' + ModelId.RADIX;
        String RELAIONAL_ENTITY = PREFIX + ":relationalEntity";
        String RELATIONSHIP = PREFIX + ":relationship";
        String SCALE = PREFIX + ':' + ModelId.SCALE;
        String SCHEMA = PREFIX + ":schema";
        String SEARCHABILITY = PREFIX + ':' + ModelId.SEARCHABILITY;
        String SELECTABLE = PREFIX + ':' + ModelId.SELECTABLE;
        String SIGNED = PREFIX + ':' + ModelId.SIGNED;
        String SUPPORTS_UPDATE = PREFIX + ':' + ModelId.SUPPORTS_UPDATE;
        String SYSTEM = PREFIX + ':' + ModelId.SYSTEM;
        String TABLE = PREFIX + ":table";
        String TABLE_HREF = PREFIX + ":tableHref";
        String TABLE_NAME = PREFIX + ":tableName";
        String TABLE_XMI_UUID = PREFIX + ":tableXmiUuid";
        String TYPE = PREFIX + ':' + ModelId.TYPE;
        String TYPE_HREF = PREFIX + ":typeHref";
        String TYPE_NAME = PREFIX + ":typeName";
        String TYPE_XMI_UUID = PREFIX + ":typeXmiUuid";
        String UNIQUE = PREFIX + ':' + ModelId.UNIQUE;
        String UNIQUE_CONSTRAINT = PREFIX + ":uniqueConstraint";
        String UNIQUE_KEY = PREFIX + ":uniqueKey";
        String UNIQUE_KEY_HREFS = PREFIX + ":uniqueKeyHrefs";
        String UNIQUE_KEY_NAMES = PREFIX + ":uniqueKeyNames";
        String UNIQUE_KEY_XMI_UUIDS = PREFIX + ":uniqueKeyXmiUuids";
        String UNIQUE_KEYS = PREFIX + ":uniqueKeys";
        String UPDATE_COUNT = PREFIX + ':' + ModelId.UPDATE_COUNT;
        String UPDATEABLE = PREFIX + ':' + ModelId.UPDATEABLE;
        String VIEW = PREFIX + ":view";
    }
}
