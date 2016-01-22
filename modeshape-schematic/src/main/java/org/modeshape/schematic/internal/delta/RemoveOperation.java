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

import org.modeshape.schematic.annotation.Immutable;
import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.HashCode;
import org.modeshape.schematic.internal.document.MutableDocument;

/**
 * An atomic remove operation for SchematicValueDelta.
 * 
 * @author (various)
 */
@Immutable
public class RemoveOperation extends Operation {
    protected final String fieldName;
    protected final Object oldValue;

    private transient boolean removed;

    public RemoveOperation( Path parentPath,
                            String fieldName,
                            Object oldValue ) {
        super(parentPath, HashCode.compute(parentPath, fieldName /*,oldValue*/));
        this.fieldName = fieldName;
        this.oldValue = oldValue;
    }

    @Override
    public RemoveOperation clone() {
        return new RemoveOperation(getParentPath(), fieldName, cloneValue(oldValue));
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (oldValue != null) {
            delegate = mutableParent(delegate);
            delegate.put(fieldName, oldValue);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        removed = parent.remove(fieldName) != null;
    }

    public boolean isRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return "Remove from '" + parentPath + "' the '" + fieldName + "' field value '" + oldValue + "'";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof RemoveOperation) {
            RemoveOperation other = (RemoveOperation)obj;
            // Note we don't consider the 'oldValue', since two removes are equivalent based only upon the field name ...
            return equalsIfNotNull(fieldName, other.fieldName) /*&& equalsIfNotNull(oldValue, other.oldValue)*/
                   && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }
}
