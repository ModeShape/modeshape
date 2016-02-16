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
import java.util.Collections;
import java.util.List;
import org.modeshape.schematic.annotation.Immutable;
import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.HashCode;
import org.modeshape.schematic.internal.document.MutableArray;
import org.modeshape.schematic.internal.document.MutableDocument;

/**
 * An atomic remove operation for SchematicValueDelta.
 * 
 * @author (various)
 */
@Immutable
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
            removedValues = new ArrayList<>(array);
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
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof ClearOperation) {
            ClearOperation that = (ClearOperation)obj;
            return equalsIfNotNull(getParentPath(), that.getParentPath());
        }
        return false;
    }
}
