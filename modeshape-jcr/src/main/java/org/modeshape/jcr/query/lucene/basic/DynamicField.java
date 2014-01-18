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
package org.modeshape.jcr.query.lucene.basic;

import java.io.Serializable;
import org.modeshape.common.annotation.Immutable;

/**
 * A representation of a field to be added to the document without knowledge of the field at compile time.
 */
@Immutable
public final class DynamicField implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String fieldName;
    private final Object value;
    private final boolean analyzed;
    private final boolean stored;
    private final DynamicField next;

    public DynamicField( DynamicField next,
                         String fieldName,
                         Object value,
                         boolean analyzed,
                         boolean stored ) {
        this.next = next;
        this.fieldName = fieldName;
        this.value = value;
        this.analyzed = analyzed;
        this.stored = stored;
    }

    /**
     * Get the field name.
     * 
     * @return the field name; never null
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Get the field value or values.
     * 
     * @return the field value or array of values; never null
     */
    public Object getValue() {
        return value;
    }

    /**
     * Determine if this field is to be analyzed.
     * 
     * @return true if the field is to be analyzed, or false otherwise
     */
    public boolean isAnalyzed() {
        return analyzed;
    }

    /**
     * Determine if this field is to be stored in the indexes, allowing use of comparison operations.
     * 
     * @return true if the field is to be stored, or false otherwise
     */
    public boolean isStored() {
        return stored;
    }

    /**
     * Get the next dynamic field.
     * 
     * @return next
     */
    public DynamicField getNext() {
        return next;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        toString(sb);
        sb.append('}');
        return sb.toString();
    }

    protected void toString( StringBuilder sb ) {
        sb.append(fieldName);
        sb.append('=').append(value);
        if (next != null) {
            sb.append(',');
            next.toString(sb);
        }
    }
}
