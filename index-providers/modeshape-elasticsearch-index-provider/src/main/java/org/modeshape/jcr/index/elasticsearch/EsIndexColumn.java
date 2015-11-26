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
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.value.PropertyType;
import static org.modeshape.jcr.value.PropertyType.BOOLEAN;
import static org.modeshape.jcr.value.PropertyType.DATE;
import static org.modeshape.jcr.value.PropertyType.LONG;
import static org.modeshape.jcr.value.PropertyType.NAME;
import static org.modeshape.jcr.value.PropertyType.REFERENCE;
import static org.modeshape.jcr.value.PropertyType.SIMPLEREFERENCE;
import static org.modeshape.jcr.value.PropertyType.URI;
import static org.modeshape.jcr.value.PropertyType.WEAKREFERENCE;
import org.modeshape.jcr.value.ValueFactories;

/**
 * Elasticsearch field definition.
 * 
 * Provides definition for elasticsearch field and implements value 
 * conversations with accordance of the column type.
 * 
 * @author kulikov
 */
public class EsIndexColumn {
    public final static String LENGTH_PREFIX = "length_";
    public final static String LOWERCASE_PREFIX = "lowercase_";
    public final static String UPPERCASE_PREFIX = "uppercase_";

    private final String name;
    private final PropertyType type;
    private final ValueFactories valueFactories;
    
    /**
     * Creates new field definition.
     * 
     * @param context ModeShape execution context
     * @param name name of this column.
     * @param type JCR type of this column.
     */
    public EsIndexColumn(ExecutionContext context, String name, int type ) {
        this.valueFactories = context.getValueFactories();
        this.name = name;
        this.type = PropertyType.valueFor(type);
    }

    /**
     * Gets the name of this column.
     * 
     * @return the name of the column.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the name of the field to store length.
     * 
     * @return 
     */
    public String getLengthFieldName() {
        return LENGTH_PREFIX + name;
    }
    
    /**
     * Gets the name of the field used for store lowered case value.
     * 
     * @return field name.
     */
    public String getLowerCaseFieldName() {
        return LOWERCASE_PREFIX + name;
    }

    /**
     * Gets the name of the field used for store upper case value.
     * 
     * @return field name.
     */
    public String getUpperCaseFieldName() {
        return UPPERCASE_PREFIX + name;
    }
    
    /**
     * Gets type of this column.
     * 
     * @return Jcr type of this column.
     */
    public PropertyType getType() {
        return type;
    }

    /**
     * Converts representation of the given value using type conversation rules
     * between JCR type of this column and Elasticsearch core type of this column.
     * 
     * @param value value to be converted.
     * @return converted value.
     */
    protected Object columnValue(Object value) {
        switch (type) {
            case PATH:
            case NAME:
            case STRING:
            case REFERENCE:
            case SIMPLEREFERENCE:
            case WEAKREFERENCE:
            case URI:
                return valueFactories.getStringFactory().create(value);
            case DATE :
                return ((DateTime) value).getMilliseconds();
            default :
                return value;
        }
    }

    protected String stringValue(Object value) {
        return valueFactories.getStringFactory().create(value);
    }
    
    /**
     * Converts array of values.
     * @param value
     * @return 
     */
    protected Object[] columnValues(Object[] value) {
        Object[] res = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            res[i] = columnValue(value[i]);
        }
        return res;
    }
    
    /**
     * Converts given value to the value of JCR type of this column.
     * 
     * @param value value to be converted.
     * @return converted value.
     */
    protected Object cast(Object value) {
        switch (type) {
            case STRING :
               return valueFactories.getStringFactory().create(value);
            case LONG :
                return valueFactories.getLongFactory().create(value);
            case NAME :
                return valueFactories.getNameFactory().create(value);
            case PATH :
                return valueFactories.getPathFactory().create(value);
            case DATE :
                return valueFactories.getDateFactory().create(value);
            case BOOLEAN :
                return valueFactories.getBooleanFactory().create(value);
            case URI :
                return valueFactories.getUriFactory().create(value);
            case REFERENCE :
                return valueFactories.getReferenceFactory().create(value);
            case SIMPLEREFERENCE :
                return valueFactories.getSimpleReferenceFactory().create(value);
            case WEAKREFERENCE :
                return valueFactories.getWeakReferenceFactory().create(value);
            default :
                return value;
        }
    }
    
    /**
     * Converts array of values.
     */
    protected Object[] cast(Object[] values) {
        Object[] res = new Object[values.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = cast(values[i]);
        }
        return res;
    }

    /**
     * Converts array of values.
     */
    protected Object[] cast(Collection values) {
        Object[] res = new Object[values.size()];
        int i = 0;
        for (Object value : values) {
            res[i++] = cast(value);
        }
        return res;
    }
    
}
