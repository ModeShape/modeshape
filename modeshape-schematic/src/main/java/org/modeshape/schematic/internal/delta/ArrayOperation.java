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

import java.util.Collection;
import org.modeshape.schematic.annotation.Immutable;
import org.modeshape.schematic.document.Array;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.document.ArrayEditor;
import org.modeshape.schematic.internal.document.MutableArray;
import org.modeshape.schematic.internal.document.MutableDocument;

/**
 * An atomic operation on an {@link Array} for SchematicValueDelta.
 * <p/>
 * 
 * @author (various)
 */
@Immutable
public abstract class ArrayOperation extends Operation {

    protected ArrayOperation( Path path,
                              int hashCode ) {
        super(path, hashCode);
    }

    @Override
    protected MutableArray mutableParent( MutableDocument delegate ) {
        Document parent = delegate;
        Path parentPath = getParentPath();
        for (String fieldName : parentPath) {
            assert parent != null : "Unexpected to find path " + parentPath + " in " + delegate + ". Unable to apply operation "
                                    + this;
            parent = parent.getDocument(fieldName);
        }
        if (parent instanceof ArrayEditor) {
            ArrayEditor parentEditor = (ArrayEditor)parent;
            parent = parentEditor.unwrap();
        }
        return (MutableArray)parent;
    }

    protected Collection<?> cloneValues( Collection<?> values ) {
        return (Collection<?>)cloneValue(values);
    }

}
