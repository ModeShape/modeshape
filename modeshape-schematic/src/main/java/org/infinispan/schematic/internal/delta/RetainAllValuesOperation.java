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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.util.Util;
import org.infinispan.schematic.document.Array.Entry;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.HashCode;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.document.MutableArray;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;

/**
 * An atomic array add operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@SerializeWith( RetainAllValuesOperation.Externalizer.class )
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

    public static final class Externalizer extends SchematicExternalizer<RetainAllValuesOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 RetainAllValuesOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeObject(put.values);
        }

        @Override
        public RetainAllValuesOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            Collection<?> values = (Collection<?>)input.readObject();
            return new RetainAllValuesOperation(path, values);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_RETAIN_ALL_VALUES_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends RetainAllValuesOperation>> getTypeClasses() {
            return Util.<Class<? extends RetainAllValuesOperation>>asSet(RetainAllValuesOperation.class);
        }
    }
}
