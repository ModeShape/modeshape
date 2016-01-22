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
 * An atomic array remove operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
public class RemoveValueOperation extends ArrayOperation {

    protected final Object value;
    protected transient int actualIndex = -1;

    public RemoveValueOperation( Path parentPath,
                                 Object value ) {
        super(parentPath, HashCode.compute(parentPath, value));
        this.value = value;
    }

    @Override
    public RemoveValueOperation clone() {
        return new RemoveValueOperation(getParentPath(), cloneValue(value));
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (actualIndex > -1) {
            MutableArray array = mutableParent(delegate);
            array.add(actualIndex, value);
        }
    }

    public Object getRemovedValue() {
        return value;
    }

    public int getActualIndex() {
        return actualIndex;
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        actualIndex = array.indexOf(value);
        array.remove(actualIndex);
    }

    @Override
    public String toString() {
        return "Remove at '" + parentPath + "' the value '" + value + "'";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof RemoveValueOperation) {
            RemoveValueOperation other = (RemoveValueOperation)obj;
            return equalsIfNotNull(value, other.value) && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }
}
