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
import java.util.stream.Collectors;
import org.modeshape.schematic.annotation.Immutable;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Path;
import org.modeshape.schematic.internal.document.MutableDocument;

/**
 * An atomic operation for SchematicValueDelta.
 * <p/>
 * 
 * @author (various)
 */
@Immutable
public abstract class Operation {

    protected final Path parentPath;
    private final int hashCode;

    protected Operation( Path parentPath,
                         int hashCode ) {
        this.parentPath = parentPath;
        this.hashCode = hashCode;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public abstract void replay( MutableDocument delegate );

    public abstract void rollback( MutableDocument delegate );

    public Path getParentPath() {
        return parentPath;
    }

    protected MutableDocument mutableParent( MutableDocument delegate ) {
        MutableDocument parent = delegate;
        Path parentPath = getParentPath();
        for (String fieldName : parentPath) {
            assert parent != null : "Unexpected to find path " + parentPath + " in " + delegate + ". Unable to apply operation "
                                    + this;
            parent = (MutableDocument)parent.getDocument(fieldName);
        }
        return parent;
    }

    @Override
    public abstract Operation clone();

    protected Object cloneValue( Object value ) {
        if (value == null) return null;
        if (value instanceof Document) return ((Document)value).clone();
        if (value instanceof Collection) {
            Collection<?> original = (Collection<?>)value;
            Collection<Object> copy = original.stream().map(this::cloneValue).collect(Collectors.toList());
            return copy;
        }
        // everything else should be immutable ...
        return value;
    }

    protected boolean equalsIfNotNull( Object obj1,
                                       Object obj2 ) {
        if (obj1 == null) {
            return obj2 == null;
        }
        return obj1.equals(obj2);
    }

}
