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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.infinispan.commons.marshall.SerializeWith;
import org.infinispan.schematic.document.Immutable;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.HashCode;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.document.MutableArray;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.marshall.Ids;
import org.infinispan.commons.util.Util;

/**
 * An atomic remove operation for SchematicValueDelta.
 * 
 * @author (various)
 * @since 5.1
 */
@Immutable
@SerializeWith( ClearOperation.Externalizer.class )
public class ClearOperation extends ArrayOperation {

    private transient List<?> removedValues;

    public ClearOperation( Path path ) {
        super(path, HashCode.compute(path));
    }

    @Override
    public ClearOperation clone() {
        return this; // immutable
    }

    @Override
    public void replay( MutableDocument delegate ) {
        MutableArray array = mutableParent(delegate);
        if (!array.isEmpty()) {
            removedValues = new ArrayList<Object>(array);
            array.clear();
        } else {
            removedValues = Collections.emptyList();
        }
    }

    @Override
    public void rollback( MutableDocument delegate ) {
        if (removedValues != null) {
            MutableArray array = mutableParent(delegate);
            array.clear();
            array.addAll(removedValues);
            removedValues = null;
        }
    }

    @Override
    public String toString() {
        return "Clear at '" + parentPath + "' the existing values";
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof ClearOperation) {
            ClearOperation that = (ClearOperation)obj;
            return equalsIfNotNull(getParentPath(), that.getParentPath());
        }
        return false;
    }

    public static final class Externalizer extends SchematicExternalizer<ClearOperation> {
        private static final long serialVersionUID = 1L;

        @Override
        public void writeObject( ObjectOutput output,
                                 ClearOperation remove ) throws IOException {
            output.writeObject(remove.parentPath);
        }

        @Override
        public ClearOperation readObject( ObjectInput input ) throws IOException, ClassNotFoundException {
            Path path = (Path)input.readObject();
            return new ClearOperation(path);
        }

        @Override
        public Integer getId() {
            return Ids.SCHEMATIC_VALUE_CLEAR_OPERATION;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public Set<Class<? extends ClearOperation>> getTypeClasses() {
            return Util.<Class<? extends ClearOperation>>asSet(ClearOperation.class);
        }
    }
}
