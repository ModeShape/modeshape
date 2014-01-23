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
 * An atomic array remove operation for SchematicValueDelta.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 */
@SerializeWith( RemoveValueOperation.Externalizer.class )
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

    public static final class Externalizer extends SchematicExternalizer<RemoveValueOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 RemoveValueOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeObject(put.value);
        }

        @Override
        public RemoveValueOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            Object value = input.readObject();
            return new RemoveValueOperation(path, value);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_REMOVE_VALUE_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends RemoveValueOperation>> getTypeClasses() {
            return Util.<Class<? extends RemoveValueOperation>>asSet(RemoveValueOperation.class);
        }
    }
}
