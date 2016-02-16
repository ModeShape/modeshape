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
public class RemoveAtIndexOperation extends ArrayOperation {

    protected final int index;
    protected transient Object actualValue = null;

    public RemoveAtIndexOperation( Path path,
                                   int index ) {
        super(path, HashCode.compute(path, index));
        this.index = index;
    }

    @Override
    public RemoveAtIndexOperation clone() {
        return new RemoveAtIndexOperation(getParentPath(), index);
    }

    public int getIndex() {
        return index;
    }

    public Object getRemovedValue() {
        return actualValue;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (actualValue != null) {
            MutableArray array = mutableParent(delegate);
            array.add(index, actualValue);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        actualValue = array.remove(index);
    }

    @Override
    public String toString() {
        return "Remove at '" + parentPath + "' the value at index " + index;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof RemoveAtIndexOperation) {
            RemoveAtIndexOperation other = (RemoveAtIndexOperation)obj;
            return index == other.index && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }
}
