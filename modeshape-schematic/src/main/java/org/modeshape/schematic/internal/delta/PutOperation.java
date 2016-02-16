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
package org.modeshape.schematic.internal.delta;

import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.HashCode;
import org.modeshape.schematic.internal.document.MutableDocument;

/**
 * An atomic put operation for SchematicValueDelta.
 * 
 * @author (various)
 */
public class PutOperation extends Operation {
    protected final String fieldName;
    protected final Object oldValue;
    protected final Object newValue;

    public PutOperation( Path parentPath,
                         String fieldName,
                         Object oldValue,
                         Object newValue ) {
        super(parentPath, HashCode.compute(parentPath, fieldName, /*oldValue,*/newValue));
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public PutOperation clone() {
        return new PutOperation(getParentPath(), fieldName, cloneValue(oldValue), cloneValue(newValue));
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        if (oldValue == null) {
            parent.remove(fieldName);
        } else {
            parent.put(fieldName, oldValue);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        parent.put(fieldName, newValue);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof PutOperation) {
            PutOperation other = (PutOperation)obj;
            return equalsIfNotNull(fieldName, other.fieldName) /*&& equalsIfNotNull(oldValue, other.oldValue)*/
                   && equalsIfNotNull(newValue, other.newValue) && equalsIfNotNull(getParentPath(), other.getParentPath());
        }
        return false;
    }

    @Override
    public String toString() {
        return "Put at '" + parentPath + "' the '" + fieldName + "' field value '" + newValue
               + (oldValue != null ? "' (replaces '" + oldValue + "')" : "'");
    }
}
