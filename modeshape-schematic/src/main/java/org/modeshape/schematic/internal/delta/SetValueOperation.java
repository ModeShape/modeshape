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
import org.modeshape.schematic.internal.document.MutableArray;
import org.modeshape.schematic.internal.document.MutableDocument;

/**
 * An atomic array add operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
public class SetValueOperation extends ArrayOperation {

    protected final Object value;
    protected final int index;

    protected transient Object oldValue;

    public SetValueOperation( Path parentPath,
                              Object value,
                              int index ) {
        super(parentPath, HashCode.compute(parentPath, value, index));
        this.value = value;
        this.index = index;
    }

    @Override
    public SetValueOperation clone() {
        return new SetValueOperation(getParentPath(), cloneValue(value), index);
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        if (oldValue != null) {
            array.set(index, oldValue);
        } else {
            array.remove(index);
        }
    }

    public int getIndex() {
        return index;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        oldValue = array.setValue(index, value);
    }

    @Override
    public String toString() {
        return "Set at '" + parentPath + "' the value '" + value + "' (at index " + index + ")";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof SetValueOperation) {
            SetValueOperation other = (SetValueOperation)obj;
            return equalsIfNotNull(value, other.value) && index == other.index
                   && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }
}
