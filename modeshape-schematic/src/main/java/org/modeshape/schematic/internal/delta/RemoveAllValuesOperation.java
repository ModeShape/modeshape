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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.modeshape.schematic.document.Array.Entry;
import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.HashCode;
import org.modeshape.schematic.internal.document.BasicArray;
import org.modeshape.schematic.internal.document.MutableArray;
import org.modeshape.schematic.internal.document.MutableDocument;

/**
 * An atomic array add operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
public class RemoveAllValuesOperation extends ArrayOperation {

    protected final Collection<?> values;
    protected transient int[] actualIndexes;

    public RemoveAllValuesOperation( Path path,
                                     Collection<?> values ) {
        super(path, HashCode.compute(path, values));
        this.values = values;
    }

    @Override
    public RemoveAllValuesOperation clone() {
        return new RemoveAllValuesOperation(getParentPath(), cloneValues(values));
    }

    public Collection<?> getValuesToRemove() {
        return values;
    }

    public synchronized List<Entry> getRemovedEntries() {
        List<Entry> entries = new ArrayList<>(this.values.size());
        Iterator<?> valueIter = values.iterator();
        for (int i = 0; i != actualIndexes.length; ++i) {
            int index = actualIndexes[i];
            Object value = valueIter.next();
            entries.add(new BasicArray.BasicEntry(index, value));
        }
        return entries;
    }

    @Override
    public synchronized void rollback( MutableDocument delegate ) {
        if (actualIndexes != null) {
            MutableArray array = mutableParent(delegate);
            // Add into the same locations ...
            int i = 0;
            for (Object value : values) {
                int index = actualIndexes[i++];
                if (index != -1) array.add(index, value);
            }
        }
    }

    @Override
    public synchronized void replay( MutableDocument delegate ) {
        if (!values.isEmpty()) {
            actualIndexes = new int[values.size()];
            int i = 0;
            MutableArray array = mutableParent(delegate);
            for (Object value : values) {
                int actualIndex = array.indexOf(value);
                array.remove(actualIndex);
                actualIndexes[i++] = actualIndex;
            }
        } else {
            actualIndexes = null;
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "Remove at '" + parentPath + "' the values: " + values;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof RemoveAllValuesOperation) {
            RemoveAllValuesOperation other = (RemoveAllValuesOperation)obj;
            return equalsIfNotNull(values, other.values) && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }
}
