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
@SerializeWith( AddValueIfAbsentOperation.Externalizer.class )
public class AddValueIfAbsentOperation extends AddValueOperation {

    protected transient boolean added;

    public AddValueIfAbsentOperation( Path path,
                                      Object value ) {
        super(path, value);
    }

    public boolean isAdded() {
        return added;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (added) {
            MutableArray array = mutableParent(delegate);
            array.remove(this.index);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        added = array.addValueIfAbsent(value);
    }

    @Override
    public String toString() {
        return super.toString() + " if absent";
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof AddValueIfAbsentOperation) {
            AddValueIfAbsentOperation other = (AddValueIfAbsentOperation)obj;
            return equalsIfNotNull(value, other.value) && index == other.index
                   && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }

    public static final class Externalizer extends SchematicExternalizer<AddValueIfAbsentOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 AddValueIfAbsentOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeObject(put.value);
        }

        @Override
        public AddValueIfAbsentOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            Object value = input.readObject();
            return new AddValueIfAbsentOperation(path, value);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_ADD_IF_ABSENT_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends AddValueIfAbsentOperation>> getTypeClasses() {
            return Util.<Class<? extends AddValueIfAbsentOperation>>asSet(AddValueIfAbsentOperation.class);
        }
    }
}
