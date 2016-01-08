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
package org.modeshape.jcr.index.elasticsearch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.index.elasticsearch.client.EsRequest;
import org.modeshape.jcr.value.PropertyType;
import static org.modeshape.jcr.value.PropertyType.BINARY;
import static org.modeshape.jcr.value.PropertyType.BOOLEAN;
import static org.modeshape.jcr.value.PropertyType.DATE;
import static org.modeshape.jcr.value.PropertyType.DECIMAL;
import static org.modeshape.jcr.value.PropertyType.DOUBLE;
import static org.modeshape.jcr.value.PropertyType.LONG;

/**
 * Set of index columns where each column provides column's type 
 * specific conversation.
 * 
 * 
 * @author kulikov
 */
public class EsIndexColumns {
    //list of columns
    private final Map<String, EsIndexColumn> columns = new HashMap<String, EsIndexColumn>();
    
    /**
     * Creates new set.
     * 
     * @param cols set of columns.
     */
    public EsIndexColumns(EsIndexColumn... cols) {
        for (int i = 0; i < cols.length; i++) {
            columns.put(cols[i].getName(), cols[i]);
        }
    }
    
    /**
     * Creates new set of columns using index definition.
     * 
     * @param context execution context 
     * @param defn index definition.
     */
    public EsIndexColumns(ExecutionContext context, IndexDefinition defn) {
        EsIndexColumn[] cols = new EsIndexColumn[defn.size()];
        for (int i = 0; i < cols.length; i++) {
            IndexColumnDefinition idef = defn.getColumnDefinition(i);
            columns.put(idef.getPropertyName(),
                    new EsIndexColumn(context, idef.getPropertyName(), idef.getColumnType()));
        }
    }
    
    /**
     * Gets column with given name.
     * 
     * @param name the name of column in index or name of pseudo column like
     * lower case, upper case or length.
     * 
     * @return real column definition related to the specified name
     */
    public EsIndexColumn column(String name) {
        return columns.get(noprefix(name, 
                EsIndexColumn.LENGTH_PREFIX,
                EsIndexColumn.LOWERCASE_PREFIX,
                EsIndexColumn.UPPERCASE_PREFIX));
    }
    
    /**
     * Cuts name prefix for the pseudo column case.
     * 
     * @param name the name of the column or pseudo column.
     * @param prefix the list of prefixes to cut.
     * @return the name without prefix.
     */
    private String noprefix(String name, String... prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (name.startsWith(prefix[i])) {
                name = name.replaceAll(prefix[i], "");
            }
        }
        return name;
    }
    
    /**
     * List of real columns.
     * 
     * @return collection of the columns.
     */
    public Collection<EsIndexColumn> columns() {
        return columns.values();
    }

    /**
     * Provides Elasticsearch mapping definition for the given type and 
     * this columns.
     * 
     * @param type the type name for wich this mapping will be applied.
     * @return mappings definition in the JSON format.
     */
    public EsRequest mappings( String type ) {
        EsRequest mappings = new EsRequest();
            EsRequest mappingsValue = new EsRequest();
                EsRequest mtype = new EsRequest();
                    EsRequest properties = new EsRequest();       
                    for (EsIndexColumn col : columns()) {
                        properties.put(col.getName(), fieldMapping(col.getType()));
                        properties.put(col.getLowerCaseFieldName(), fieldMapping(PropertyType.STRING));
                        properties.put(col.getUpperCaseFieldName(), fieldMapping(PropertyType.STRING));
                        properties.put(col.getLengthFieldName(), fieldMapping(PropertyType.LONG));
                    }        
                mtype.put("properties", properties);
            mappingsValue.put(type, mtype);
        mappings.put("mappings", mappingsValue);
        
        return mappings;
    }
    
    /**
     * Creates mapping definition for the specified column type.
     * 
     * @param type
     * @return 
     */
    private EsRequest fieldMapping( PropertyType type ) {
        EsRequest mappings = new EsRequest();
        switch (type) {
            case BINARY:
                mappings.put("type", "binary");
                break;
            case BOOLEAN:
                mappings.put("type", "boolean");
                break;
            case DATE:
                mappings.put("type", "long");
                break;
            case LONG:
            case DECIMAL:
                mappings.put("type", "long");
                break;
            case DOUBLE:
                mappings.put("type", "double");
                break;
            default:
                mappings.put("type", "string");
                mappings.put("analyzer", "whitespace");
        }
        return mappings;
    }
    
}
