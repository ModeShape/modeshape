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
package org.modeshape.jdbc.metadata;

/**
 * This constants in this class indicate the column positions of different columns in queries against metadata. These constants
 * are used in JcrResultsMetaData object to obtain column specific metadata information.
 */

public interface ResultsMetadataConstants {

    // constant indicating the position of catalog .
    public static final Integer CATALOG = new Integer(0);
    // constant indicating the position of schema .
    public static final Integer SCHEMA = new Integer(1);
    // constant indicating the position of table.
    public static final Integer TABLE = new Integer(2);
    // constant indicating the position of column .
    public static final Integer COLUMN = new Integer(3);
    // constant indicating the position of column label used for display purposes.
    public static final Integer COLUMN_LABEL = new Integer(4);
    // constant indicating the position of datatype of the column.
    public static final Integer DATA_TYPE = new Integer(6);
    // constant indicating the position of precision of the column.
    public static final Integer PRECISION = new Integer(7);
    // constant indiacting the position of radix of a column.
    public static final Integer RADIX = new Integer(8);
    // constant indicating scale of the column.
    public static final Integer SCALE = new Integer(9);
    // constant indiacting the position of auto-incrementable property of a column.
    public static final Integer AUTO_INCREMENTING = new Integer(10);
    // constant indiacting the position of columns case sensitivity.
    public static final Integer CASE_SENSITIVE = new Integer(11);
    // constant indicating the position of nullable property of a column.
    public static final Integer NULLABLE = new Integer(12);
    // constant indicating the position of searchable property of a column.
    public static final Integer SEARCHABLE = new Integer(13);
    // constant indicating the position of signed property of a column.
    public static final Integer SIGNED = new Integer(14);
    // constant indicating the position of updatable property of a column.
    public static final Integer WRITABLE = new Integer(15);
    // constant indicating if a column is a currency value
    public static final Integer CURRENCY = new Integer(16);
    // constant indicating the display size for a column
    public static final Integer DISPLAY_SIZE = new Integer(17);

    /**
     * These types are associated with a DataType or an Element needing the indication of null types.
     */
    public static final class NULL_TYPES {
        public static final Integer NOT_NULL = new Integer(1);
        public static final Integer NULLABLE = new Integer(2);
        public static final Integer UNKNOWN = new Integer(3);
    }

    /**
     * These types are associated with the Element having valid search types.
     */
    public static final class SEARCH_TYPES {
        public static final Integer SEARCHABLE = new Integer(1);
        public static final Integer ALLEXCEPTLIKE = new Integer(2);
        public static final Integer LIKE_ONLY = new Integer(3);
        public static final Integer UNSEARCHABLE = new Integer(4);
    }

    public static final class TABLE_TYPES {
        public static final String VIEW = "VIEW";
    }

}
