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
package org.infinispan.schematic.internal.delta;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.schematic.document.Array.Entry;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.HashCode;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.document.BasicArray;
import org.infinispan.schematic.internal.document.MutableArray;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;

/**
 * An atomic array add operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@SerializeWith( RemoveAllValuesOperation.Externalizer.class )
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
        List<Entry> entries = new ArrayList<Entry>(this.values.size());
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

    public static final class Externalizer extends SchematicExternalizer<RemoveAllValuesOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 RemoveAllValuesOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeObject(put.values);
        }

        @Override
        public RemoveAllValuesOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            Collection<?> values = (Collection<?>)input.readObject();
            return new RemoveAllValuesOperation(path, values);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_REMOVE_ALL_VALUES_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends RemoveAllValuesOperation>> getTypeClasses() {
            return Collections.<Class<? extends RemoveAllValuesOperation>>singleton(RemoveAllValuesOperation.class);
        }
    }
}
