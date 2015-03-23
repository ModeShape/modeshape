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
import java.util.Collections;
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
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
@SerializeWith( RemoveAtIndexOperation.Externalizer.class )
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

    public static final class Externalizer extends SchematicExternalizer<RemoveAtIndexOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 RemoveAtIndexOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeObject(put.index);
        }

        @Override
        public RemoveAtIndexOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            int index = input.readInt();
            return new RemoveAtIndexOperation(path, index);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_REMOVE_AT_INDEX_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends RemoveAtIndexOperation>> getTypeClasses() {
            return Collections.<Class<? extends RemoveAtIndexOperation>>singleton(RemoveAtIndexOperation.class);
        }
    }
}
