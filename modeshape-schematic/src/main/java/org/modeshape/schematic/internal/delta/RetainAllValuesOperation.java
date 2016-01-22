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

import java.util.Collection;
import java.util.List;
import org.modeshape.schematic.document.Array.Entry;
import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.HashCode;
import org.modeshape.schematic.internal.document.MutableArray;
import org.modeshape.schematic.internal.document.MutableDocument;

/**
 * An atomic array add operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
public class RetainAllValuesOperation extends ArrayOperation {

    protected final Collection<?> values;
    protected transient List<Entry> removedEntries;

    public RetainAllValuesOperation( Path parentPath,
                                     Collection<?> values ) {
        super(parentPath, HashCode.compute(parentPath, values));
        this.values = values;
    }

    @Override
    public RetainAllValuesOperation clone() {
        return new RetainAllValuesOperation(getParentPath(), cloneValues(values));
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (removedEntries != null) {
            // Add into the same locations ...
            MutableArray array = mutableParent(delegate);
            for (Entry entry : removedEntries) {
                array.add(entry.getIndex(), entry.getValue());
            }
        }
    }

    public Collection<?> getRetainedValues() {
        return values;
    }

    public List<Entry> getRemovedEntries() {
        return removedEntries;
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        removedEntries = array.retainAllValues(values);
    }

    @Override
    public String toString() {
        return "Retain at '" + parentPath + "' the values: " + values;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof RetainAllValuesOperation) {
            RetainAllValuesOperation other = (RetainAllValuesOperation)obj;
            return equalsIfNotNull(values, other.values) && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }
}
