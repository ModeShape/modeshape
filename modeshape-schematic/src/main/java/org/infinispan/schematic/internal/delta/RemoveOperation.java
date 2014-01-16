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
import org.infinispan.schematic.document.Immutable;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.HashCode;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.commons.util.Util;

/**
 * An atomic remove operation for SchematicValueDelta.
 * 
 * @author (various)
 */
@Immutable
@SerializeWith( RemoveOperation.Externalizer.class )
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
    public boolean equals( Object obj ) {
        if (obj instanceof RemoveOperation) {
            RemoveOperation other = (RemoveOperation)obj;
            // Note we don't consider the 'oldValue', since two removes are equivalent based only upon the field name ...
            return equalsIfNotNull(fieldName, other.fieldName) /*&& equalsIfNotNull(oldValue, other.oldValue)*/
                   && equalsIfNotNull(getParentPath(), other.getParentPath());

        }
        return false;
    }

    public static final class Externalizer extends SchematicExternalizer<RemoveOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 RemoveOperation remove ) throws IOException {
            output.writeObject(remove.parentPath);
            output.writeUTF(remove.fieldName);
        }

        @Override
        public RemoveOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path parentPath = (Path)input.readObject();
            String fieldName = input.readUTF();
            return new RemoveOperation(parentPath, fieldName, null);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_REMOVE_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends RemoveOperation>> getTypeClasses() {
            return Util.<Class<? extends RemoveOperation>>asSet(RemoveOperation.class);
        }
    }
}
