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
