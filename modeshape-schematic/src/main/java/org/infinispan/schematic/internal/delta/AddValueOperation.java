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
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.commons.util.Util;
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
@SerializeWith( AddValueOperation.Externalizer.class )
public class AddValueOperation extends ArrayOperation {

    protected static final int APPEND_INDEX = -1;

    protected final Object value;
    protected final int index;
    protected transient int actualIndex = -1;

    public AddValueOperation( Path path,
                              Object value ) {
        super(path, HashCode.compute(path, value, APPEND_INDEX));
        assert value != null;
        this.value = value;
        this.index = APPEND_INDEX;
    }

    public AddValueOperation( Path path,
                              Object value,
                              int index ) {
        super(path, HashCode.compute(path, value, index));
        assert value != null;
        this.value = value;
        this.index = index;
    }

    @Override
    public AddValueOperation clone() {
        return new AddValueOperation(getParentPath(), cloneValue(value), index);
    }

    public Object getValue() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    public int getActualIndex() {
        return actualIndex;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (actualIndex > -1) {
            MutableArray array = mutableParent(delegate);
            array.remove(actualIndex);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        if (index == APPEND_INDEX) {
            actualIndex = array.addValue(value);
        } else {
            array.addValue(index, value);
            actualIndex = index;
        }
    }

    @Override
    public String toString() {
        return "Add to '" + parentPath + "' the value '" + value + "'" + (index >= 0 ? " at index " + index : "");
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof AddValueOperation) {
            AddValueOperation other = (AddValueOperation)obj;
            return equalsIfNotNull(value, other.value) && index == other.index
                   && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }

    public static final class Externalizer extends SchematicExternalizer<AddValueOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 AddValueOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeInt(put.index);
            output.writeObject(put.value);
        }

        @Override
        public AddValueOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            int index = input.readInt();
            Object value = input.readObject();
            return new AddValueOperation(path, value, index);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_ADD_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends AddValueOperation>> getTypeClasses() {
            return Util.<Class<? extends AddValueOperation>>asSet(AddValueOperation.class);
        }
    }
}
