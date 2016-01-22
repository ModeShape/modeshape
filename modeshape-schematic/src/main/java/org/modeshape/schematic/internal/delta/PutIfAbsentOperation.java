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
 * An atomic put-if-absent operation for SchematicValueDelta.
 * 
 * @author (various)
 */
public class PutIfAbsentOperation extends Operation {
    protected final String fieldName;
    protected final Object newValue;
    private transient boolean absent = false;

    public PutIfAbsentOperation( Path parentPath,
                                 String fieldName,
                                 Object newValue ) {
        super(parentPath, HashCode.compute(parentPath, fieldName, newValue));
        this.fieldName = fieldName;
        this.newValue = newValue;
    }

    @Override
    public PutIfAbsentOperation clone() {
        return new PutIfAbsentOperation(getParentPath(), fieldName, cloneValue(newValue));
    }

    public Object getNewValue() {
        return newValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isApplied() {
        return absent;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        if (absent) {
            parent.remove(fieldName);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        if (!parent.containsField(fieldName)) {
            parent.put(fieldName, newValue);
            absent = true;
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof PutIfAbsentOperation) {
            PutIfAbsentOperation other = (PutIfAbsentOperation)obj;
            return equalsIfNotNull(fieldName, other.fieldName) && absent == other.absent
                   && equalsIfNotNull(newValue, other.newValue) && getParentPath().equals(other.getParentPath());

        }
        return false;
    }

    @Override
    public String toString() {
        return "Put-if-absent at '" + parentPath + "' the '" + fieldName + "' field value '" + newValue + "'";
    }
}
