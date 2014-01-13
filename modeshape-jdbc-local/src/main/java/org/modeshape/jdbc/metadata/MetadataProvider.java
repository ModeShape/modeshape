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

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.jdbc.JcrType;

/**
 */
public class MetadataProvider {

    // Map of detail maps -- <columnIndex, Map<propertyName, metadataObject>>
    protected Map<?, Object>[] metadata;

    public MetadataProvider( Map<?, Object>[] metadata ) {
        this.metadata = metadata;
    }

    public Object getValue( int columnIndex,
                            Integer metadataPropertyKey ) {
        if (columnIndex < 0 || columnIndex >= metadata.length) {
            assert (columnIndex < 0 || columnIndex >= metadata.length);
        }

        Map<?, Object> column = this.metadata[columnIndex];
        return column.get(metadataPropertyKey);
    }

    public int getColumnCount() {
        return metadata.length;
    }

    public String getStringValue( int columnIndex,
                                  Integer metadataPropertyKey ) {
        return (String)getValue(columnIndex, metadataPropertyKey);
    }

    public int getIntValue( int columnIndex,
                            Integer metadataPropertyKey ) {
        return ((Integer)getValue(columnIndex, metadataPropertyKey)).intValue();
    }

    public boolean getBooleanValue( int columnIndex,
                                    Integer metadataPropertyKey ) {
        return ((Boolean)getValue(columnIndex, metadataPropertyKey)).booleanValue();
    }

    public static Map<Integer, Object> getColumnMetadata( String catalogName,
                                                          String tableName,
                                                          String columnName,
                                                          String dataType,
                                                          Integer nullable,
                                                          Connection driverConnection ) {
        return getColumnMetadata(catalogName,
                                 tableName,
                                 columnName,
                                 dataType,
                                 nullable,
                                 ResultsMetadataConstants.SEARCH_TYPES.UNSEARCHABLE,
                                 Boolean.FALSE,
                                 Boolean.FALSE,
                                 Boolean.FALSE,
                                 driverConnection);
    }

    public static Map<Integer, Object> getColumnMetadata( String catalogName,
                                                          String tableName,
                                                          String columnName,
                                                          String dataType,
                                                          Integer nullable,
                                                          Integer searchable,
                                                          Boolean writable,
                                                          Boolean signed,
                                                          Boolean caseSensitive,
                                                          Connection driverConnection ) {

        // map that would contain metadata details
        Map<Integer, Object> metadataMap = new HashMap<Integer, Object>();

        /*******************************************************
         * HardCoding Column metadata details for the given column
         ********************************************************/

        JcrType type = JcrType.typeInfo(dataType);
        if (type == null) {
            throw new RuntimeException("Program error: jcr type " + dataType + " not found");
        }

        metadataMap.put(ResultsMetadataConstants.CATALOG, catalogName);
        metadataMap.put(ResultsMetadataConstants.TABLE, tableName);
        metadataMap.put(ResultsMetadataConstants.COLUMN, columnName);
        metadataMap.put(ResultsMetadataConstants.DATA_TYPE, dataType);
        metadataMap.put(ResultsMetadataConstants.PRECISION, type.getDefaultPrecision());
        metadataMap.put(ResultsMetadataConstants.RADIX, new Integer(10));
        metadataMap.put(ResultsMetadataConstants.SCALE, new Integer(0));
        metadataMap.put(ResultsMetadataConstants.AUTO_INCREMENTING, Boolean.FALSE);
        metadataMap.put(ResultsMetadataConstants.CASE_SENSITIVE, caseSensitive);
        metadataMap.put(ResultsMetadataConstants.NULLABLE, nullable);
        metadataMap.put(ResultsMetadataConstants.SEARCHABLE, searchable);
        metadataMap.put(ResultsMetadataConstants.SIGNED, signed);
        metadataMap.put(ResultsMetadataConstants.WRITABLE, writable);
        metadataMap.put(ResultsMetadataConstants.CURRENCY, Boolean.FALSE);
        metadataMap.put(ResultsMetadataConstants.DISPLAY_SIZE, type.getNominalDisplaySize());

        return metadataMap;
    }

}
