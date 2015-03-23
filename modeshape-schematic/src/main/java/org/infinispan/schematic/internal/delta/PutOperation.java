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
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;

/**
 * An atomic put operation for SchematicValueDelta.
 * 
 * @author (various)
 */
@SerializeWith( PutOperation.Externalizer.class )
public class PutOperation extends Operation {
    protected final String fieldName;
    protected final Object oldValue;
    protected final Object newValue;

    public PutOperation( Path parentPath,
                         String fieldName,
                         Object oldValue,
                         Object newValue ) {
        super(parentPath, HashCode.compute(parentPath, fieldName, /*oldValue,*/newValue));
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public PutOperation clone() {
        return new PutOperation(getParentPath(), fieldName, cloneValue(oldValue), cloneValue(newValue));
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        if (oldValue == null) {
            parent.remove(fieldName);
        } else {
            parent.put(fieldName, oldValue);
        }
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableDocument parent = mutableParent(delegate);
        assert parent != null;
        parent.put(fieldName, newValue);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof PutOperation) {
            PutOperation other = (PutOperation)obj;
            return equalsIfNotNull(fieldName, other.fieldName) /*&& equalsIfNotNull(oldValue, other.oldValue)*/
                   && equalsIfNotNull(newValue, other.newValue) && equalsIfNotNull(getParentPath(), other.getParentPath());
        }
        return false;
    }

    @Override
    public String toString() {
        return "Put at '" + parentPath + "' the '" + fieldName + "' field value '" + newValue
               + (oldValue != null ? "' (replaces '" + oldValue + "')" : "'");
    }

    public static final class Externalizer extends SchematicExternalizer<PutOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 PutOperation put ) throws IOException {
            output.writeObject(put.parentPath);
            output.writeUTF(put.fieldName);
            output.writeObject(put.newValue);
        }

        @Override
        public PutOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            String fieldName = input.readUTF();
            Object newValue = input.readObject();
            return new PutOperation(path, fieldName, null, newValue);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_PUT_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends PutOperation>> getTypeClasses() {
            return Collections.<Class<? extends PutOperation>>singleton(PutOperation.class);
        }
    }
}
